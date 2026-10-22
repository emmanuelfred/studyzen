package com.fredrickemmanuel.studyzen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.ExifInterface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import android.util.Base64
import com.fredrickemmanuel.studyzen.feature.media.MediaPlatform
import com.fredrickemmanuel.studyzen.feature.media.RecordedAudio
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.max

/** Android's side of notes media and pronunciation. Created in onCreate (it registers launchers). */
class AndroidMediaPlatform(private val activity: ComponentActivity) : MediaPlatform {
    override val supported = true

    private val dir = File(activity.filesDir, "note_media").apply { mkdirs() }

    private var imageCallback: ((String?) -> Unit)? = null
    private var micCallback: ((Boolean) -> Unit)? = null

    private val imagePicker =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val callback = imageCallback
            imageCallback = null
            if (uri == null) {
                callback?.invoke(null)
            } else {
                // decoding a big photo takes a moment: do it off the main thread
                thread {
                    val name = runCatching { importImage(uri) }.getOrNull()
                    activity.runOnUiThread { callback?.invoke(name) }
                }
            }
        }

    private var cameraFile: File? = null

    private val cameraLauncher =
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
            val callback = imageCallback
            imageCallback = null
            val file = cameraFile
            cameraFile = null
            if (!taken || file == null) {
                file?.delete()
                callback?.invoke(null)
            } else {
                thread {
                    val name = runCatching { importImage(Uri.fromFile(file)) }.getOrNull()
                    file.delete()
                    activity.runOnUiThread { callback?.invoke(name) }
                }
            }
        }

    private val micPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val callback = micCallback
            micCallback = null
            callback?.invoke(granted && beginRecording())
        }

    // --- Photos ---------------------------------------------------------------------------

    override fun pickImage(onResult: (String?) -> Unit) {
        imageCallback = onResult
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun captureImage(onResult: (String?) -> Unit) {
        try {
            val folder = File(activity.cacheDir, "camera").apply { mkdirs() }
            val file = File(folder, "shot_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            cameraFile = file
            imageCallback = onResult
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            // no camera app, or the launch failed
            cameraFile = null
            imageCallback = null
            onResult(null)
        }
    }

    override fun readImageBase64(fileName: String): String? {
        val file = File(dir, fileName)
        if (!file.exists()) return null
        return runCatching { Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) }.getOrNull()
    }

    /** Copies the chosen photo into private storage, shrunk to at most 1600 px and upright. */
    private fun importImage(uri: Uri): String? {
        val resolver = activity.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 3200) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val rotation = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = minOf(1f, 1600f / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val name = "img_${System.currentTimeMillis()}.jpg"
        FileOutputStream(File(dir, name)).use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        return name
    }

    override fun loadImage(fileName: String): ImageBitmap? {
        val file = File(dir, fileName)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
    }

    // --- Recording ------------------------------------------------------------------------

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStart = 0L

    override fun startRecording(onStarted: (Boolean) -> Unit) {
        stopPlayback()
        val has = activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (has) {
            onStarted(beginRecording())
        } else {
            micCallback = onStarted
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun beginRecording(): Boolean {
        cancelRecording()
        return try {
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(activity) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingFile = file
            recordingStart = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            cancelRecording()
            false
        }
    }

    override fun stopRecording(): RecordedAudio? {
        val r = recorder ?: return null
        val file = recordingFile
        val length = System.currentTimeMillis() - recordingStart
        recorder = null
        recordingFile = null
        val stopped = runCatching { r.stop() }.isSuccess // fails when it was too short
        runCatching { r.release() }
        if (!stopped || file == null || length < 700) {
            file?.delete()
            return null
        }
        return RecordedAudio(file.name, length)
    }

    override fun cancelRecording() {
        val r = recorder
        recorder = null
        if (r != null) {
            runCatching { r.stop() }
            runCatching { r.release() }
        }
        recordingFile?.delete()
        recordingFile = null
    }

    // --- Playing and speaking ---------------------------------------------------------------

    private var player: MediaPlayer? = null

    private fun newPlayer(): MediaPlayer {
        stopPlayback()
        return MediaPlayer().also {
            it.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            player = it
        }
    }

    override fun playFile(fileName: String, onDone: () -> Unit) {
        val file = File(dir, fileName)
        if (!file.exists()) {
            onDone()
            return
        }
        try {
            val p = newPlayer()
            p.setDataSource(file.absolutePath)
            p.setOnCompletionListener { stopPlayback(); onDone() }
            p.setOnErrorListener { _, _, _ -> stopPlayback(); onDone(); true }
            p.prepare()
            p.start()
        } catch (e: Exception) {
            stopPlayback()
            onDone()
        }
    }

    override fun playUrl(url: String, onDone: () -> Unit, onError: () -> Unit) {
        try {
            val p = newPlayer()
            p.setDataSource(url)
            p.setOnPreparedListener { it.start() }
            p.setOnCompletionListener { stopPlayback(); onDone() }
            p.setOnErrorListener { _, _, _ -> stopPlayback(); onError(); true }
            p.prepareAsync()
        } catch (e: Exception) {
            stopPlayback()
            onError()
        }
    }

    override fun stopPlayback() {
        val p = player
        player = null
        if (p != null) {
            p.setOnCompletionListener(null)
            p.setOnErrorListener(null)
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        tts?.stop()
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var waitingText: String? = null

    override fun speak(text: String) {
        stopPlayback()
        if (tts == null) {
            waitingText = text
            tts = TextToSpeech(activity) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.language = Locale.US
                    waitingText?.let { say(it) }
                }
                waitingText = null
            }
        } else if (ttsReady) {
            say(text)
        }
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "studyzen-word")
    }

    // --- Files --------------------------------------------------------------------------------

    override fun deleteMedia(fileName: String) {
        runCatching { File(dir, fileName).delete() }
    }

    override fun hasMedia(fileName: String): Boolean = File(dir, fileName).exists()

    /** Call from the activity's onDestroy. */
    fun release() {
        cancelRecording()
        stopPlayback()
        tts?.shutdown()
        tts = null
    }
}
