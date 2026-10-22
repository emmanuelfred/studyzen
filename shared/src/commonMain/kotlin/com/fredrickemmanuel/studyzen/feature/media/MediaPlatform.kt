package com.fredrickemmanuel.studyzen.feature.media

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** A finished voice recording, saved on the device. */
data class RecordedAudio(val fileName: String, val durationMs: Long)

/**
 * Everything that needs the phone itself: picking photos, recording and playing sound, and
 * reading a word aloud. Each platform provides its own version; [NoMediaPlatform] does nothing,
 * so the rest of the app still works (without these buttons) where a platform has no support yet.
 *
 * Photos and recordings are kept in the app's private storage and referred to by file name.
 */
interface MediaPlatform {
    /** False when this platform cannot pick photos or record/play sound. */
    val supported: Boolean

    /** Lets the learner choose a photo. [onResult] gets the saved file name, or null if cancelled. */
    fun pickImage(onResult: (String?) -> Unit)

    /** Opens the camera. [onResult] gets the saved file name, or null if cancelled or there is no camera. */
    fun captureImage(onResult: (String?) -> Unit)

    /** The saved photo as base64 text (for sending to an AI), or null if it is missing. */
    fun readImageBase64(fileName: String): String?

    /** Loads a saved photo, or null if the file is not on this device. */
    fun loadImage(fileName: String): ImageBitmap?

    /** Starts recording (asking for microphone permission first). [onStarted] says whether it began. */
    fun startRecording(onStarted: (Boolean) -> Unit)

    /** Stops and saves the recording. Null when it was too short or failed. */
    fun stopRecording(): RecordedAudio?
    fun cancelRecording()

    /** Plays a saved recording. [onDone] is called when it ends (not when stopped by [stopPlayback]). */
    fun playFile(fileName: String, onDone: () -> Unit)

    /** Plays a sound from the internet, such as a word's pronunciation. [onError] if it cannot play. */
    fun playUrl(url: String, onDone: () -> Unit, onError: () -> Unit)

    /** Reads [text] aloud with the phone's own voice. Works offline. */
    fun speak(text: String)

    fun stopPlayback()
    fun deleteMedia(fileName: String)
    fun hasMedia(fileName: String): Boolean
}

object NoMediaPlatform : MediaPlatform {
    override val supported = false
    override fun pickImage(onResult: (String?) -> Unit) = onResult(null)
    override fun captureImage(onResult: (String?) -> Unit) = onResult(null)
    override fun readImageBase64(fileName: String): String? = null
    override fun loadImage(fileName: String): ImageBitmap? = null
    override fun startRecording(onStarted: (Boolean) -> Unit) = onStarted(false)
    override fun stopRecording(): RecordedAudio? = null
    override fun cancelRecording() = Unit
    override fun playFile(fileName: String, onDone: () -> Unit) = onDone()
    override fun playUrl(url: String, onDone: () -> Unit, onError: () -> Unit) = onError()
    override fun speak(text: String) = Unit
    override fun stopPlayback() = Unit
    override fun deleteMedia(fileName: String) = Unit
    override fun hasMedia(fileName: String) = false
}

val LocalMediaPlatform = staticCompositionLocalOf<MediaPlatform> { NoMediaPlatform }
