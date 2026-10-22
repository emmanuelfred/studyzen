package com.fredrickemmanuel.studyzen

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.fredrickemmanuel.studyzen.feature.media.MediaPlatform
import com.fredrickemmanuel.studyzen.feature.media.RecordedAudio
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.sound.sampled.TargetDataLine
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max

/**
 * Desktop version of the photo and voice features, built only from what the JDK already includes:
 * a file dialog for photos, the microphone for voice notes and the speakers for playback.
 * Files live in ~/.studyzen/note_media. There is no camera on desktop, so "take a photo" opens
 * the same file chooser.
 */
class DesktopMediaPlatform : MediaPlatform {
    override val supported = true

    private val dir = File(System.getProperty("user.home"), ".studyzen/note_media").apply { mkdirs() }

    // ---- photos ----

    override fun pickImage(onResult: (String?) -> Unit) {
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply {
                dialogTitle = "Choose a photo"
                fileFilter = FileNameExtensionFilter("Images (png, jpg, gif)", "png", "jpg", "jpeg", "gif", "bmp")
            }
            val picked = if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
            if (picked == null) {
                onResult(null)
            } else {
                Thread { onResult(runCatching { importImage(picked) }.getOrNull()) }.start()
            }
        }
    }

    override fun captureImage(onResult: (String?) -> Unit) = pickImage(onResult)

    /** Shrinks the photo to at most 1600 px and saves it as a JPEG, like the phone version does. */
    private fun importImage(source: File): String? {
        val original = ImageIO.read(source) ?: return null
        val scale = minOf(1.0, 1600.0 / max(original.width, original.height))
        val width = max(1, (original.width * scale).toInt())
        val height = max(1, (original.height * scale).toInt())
        val rgb = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = rgb.createGraphics()
        g.drawImage(original.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, java.awt.Color.WHITE, null)
        g.dispose()
        val name = "img_${System.currentTimeMillis()}.jpg"
        return if (ImageIO.write(rgb, "jpg", File(dir, name))) name else null
    }

    override fun readImageBase64(fileName: String): String? {
        val file = File(dir, fileName)
        if (!file.exists()) return null
        return runCatching { Base64.getEncoder().encodeToString(file.readBytes()) }.getOrNull()
    }

    override fun loadImage(fileName: String): ImageBitmap? {
        val file = File(dir, fileName)
        if (!file.exists()) return null
        return runCatching { file.inputStream().use { loadImageBitmap(it) } }.getOrNull()
    }

    // ---- voice notes ----

    private val format = AudioFormat(16_000f, 16, 1, true, false)
    private var line: TargetDataLine? = null
    private var recorder: Thread? = null
    private var recorded: ByteArrayOutputStream? = null

    override fun startRecording(onStarted: (Boolean) -> Unit) {
        cancelRecording()
        val started = runCatching {
            val l = AudioSystem.getTargetDataLine(format)
            l.open(format)
            l.start()
            val buffer = ByteArrayOutputStream()
            line = l
            recorded = buffer
            recorder = Thread {
                val chunk = ByteArray(4096)
                while (l.isOpen) {
                    val n = l.read(chunk, 0, chunk.size)
                    if (n > 0) synchronized(buffer) { buffer.write(chunk, 0, n) } else if (n < 0) break
                }
            }.apply { isDaemon = true; start() }
        }.isSuccess
        if (!started) cancelRecording()
        onStarted(started)
    }

    override fun stopRecording(): RecordedAudio? {
        val l = line ?: return null
        val buffer = recorded ?: return null
        closeLine(l)
        recorder?.join(500)
        line = null; recorder = null; recorded = null
        val bytes = synchronized(buffer) { buffer.toByteArray() }
        val frames = bytes.size / format.frameSize
        val durationMs = frames * 1000L / format.frameRate.toLong()
        if (durationMs < 500) return null
        val name = "voice_${System.currentTimeMillis()}.wav"
        return runCatching {
            AudioInputStream(ByteArrayInputStream(bytes), format, frames.toLong()).use {
                AudioSystem.write(it, AudioFileFormat.Type.WAVE, File(dir, name))
            }
            RecordedAudio(name, durationMs)
        }.getOrNull()
    }

    override fun cancelRecording() {
        line?.let { closeLine(it) }
        line = null; recorder = null; recorded = null
    }

    private fun closeLine(l: TargetDataLine) {
        runCatching { l.stop() }
        runCatching { l.close() }
    }

    // ---- playback and speech ----

    private var clip: Clip? = null
    private var stoppedByUser = false

    override fun playFile(fileName: String, onDone: () -> Unit) {
        stopPlayback()
        val file = File(dir, fileName)
        val c = runCatching {
            val stream = AudioSystem.getAudioInputStream(file)
            AudioSystem.getClip().also { it.open(stream) }
        }.getOrNull()
        if (c == null) {
            onDone()
            return
        }
        stoppedByUser = false
        clip = c
        c.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                c.close()
                if (clip === c) clip = null
                if (!stoppedByUser) onDone()
            }
        }
        c.start()
    }

    /** Desktop Java cannot play MP3 links, so the dictionary falls back to reading the word aloud. */
    override fun playUrl(url: String, onDone: () -> Unit, onError: () -> Unit) = onError()

    override fun speak(text: String) {
        val clean = text.trim().take(500)
        if (clean.isEmpty()) return
        Thread {
            runCatching {
                val os = System.getProperty("os.name").lowercase()
                val command = when {
                    "mac" in os -> listOf("say", clean)
                    "win" in os -> listOf(
                        "powershell", "-NoProfile", "-Command",
                        "Add-Type -AssemblyName System.Speech; " +
                            "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak(\$env:STUDYZEN_SPEAK)",
                    )
                    else -> listOf("espeak", clean)
                }
                ProcessBuilder(command).apply { environment()["STUDYZEN_SPEAK"] = clean }.start().waitFor()
            }
        }.apply { isDaemon = true; start() }
    }

    override fun stopPlayback() {
        val c = clip ?: return
        stoppedByUser = true
        clip = null
        runCatching { c.stop(); c.close() }
    }

    override fun deleteMedia(fileName: String) {
        runCatching { File(dir, fileName).delete() }
    }

    override fun hasMedia(fileName: String) = File(dir, fileName).exists()
}
