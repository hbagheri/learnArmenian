package com.learnarm.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRecorderSpeechRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechRecorder {

    private val recordingDir: File by lazy {
        File(context.cacheDir, "speech").apply { mkdirs() }
    }

    @Volatile
    private var recorder: MediaRecorder? = null

    @Volatile
    private var currentFile: File? = null

    override val isRecording: Boolean
        get() = recorder != null

    override fun start(): Boolean {
        if (recorder != null) return false
        val outFile = File(recordingDir, "rec_${System.currentTimeMillis()}.m4a")
        val mr = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(SAMPLE_RATE_HZ)
            setAudioEncodingBitRate(BIT_RATE_BPS)
            setOutputFile(outFile.absolutePath)
        }
        return try {
            mr.prepare()
            mr.start()
            recorder = mr
            currentFile = outFile
            true
        } catch (t: Throwable) {
            runCatching { mr.release() }
            outFile.delete()
            false
        }
    }

    override fun stop(): File? {
        val mr = recorder ?: return null
        val file = currentFile
        recorder = null
        currentFile = null
        return try {
            mr.stop()
            file
        } catch (_: Throwable) {
            file?.delete()
            null
        } finally {
            runCatching { mr.release() }
        }
    }

    override fun cancel() {
        val mr = recorder
        val file = currentFile
        recorder = null
        currentFile = null
        if (mr != null) {
            runCatching { mr.stop() }
            runCatching { mr.release() }
        }
        file?.delete()
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val BIT_RATE_BPS = 96_000
    }
}
