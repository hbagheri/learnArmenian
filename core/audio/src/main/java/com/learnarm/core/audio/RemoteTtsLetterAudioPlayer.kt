package com.learnarm.core.audio

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class RemoteTtsLetterAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val config: AudioConfig,
) : LetterAudioPlayer {

    private val cacheDir: File by lazy {
        File(context.cacheDir, "letter_audio").apply { mkdirs() }
    }

    @Volatile
    private var activePlayer: MediaPlayer? = null

    override suspend fun play(request: AudioRequest): PlayResult {
        return try {
            val file = ensureCached(request)
            playFile(file)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (io: IOException) {
            PlayResult.Failed(PlayResult.Reason.Network, io)
        } catch (t: Throwable) {
            PlayResult.Failed(PlayResult.Reason.Decode, t)
        }
    }

    override fun stop() {
        activePlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        activePlayer = null
    }

    private suspend fun ensureCached(request: AudioRequest): File = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, cacheKey(request) + ".mp3")
        if (cacheFile.exists() && cacheFile.length() > 0) return@withContext cacheFile

        val body = Json.encodeToString(
            TtsRequest.serializer(),
            TtsRequest(text = request.text, voice = request.voice),
        ).toRequestBody(JSON_MEDIA_TYPE)

        val httpRequest = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/tts")
            .post(body)
            .build()

        httpClient.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("TTS request failed: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: throw IOException("TTS response had empty body")
            val tmp = File(cacheDir, cacheFile.name + ".part")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(cacheFile)) {
                cacheFile.writeBytes(bytes)
                tmp.delete()
            }
        }
        cacheFile
    }

    private suspend fun playFile(file: File): PlayResult = suspendCancellableCoroutine { cont ->
        stop()
        val player = MediaPlayer()
        activePlayer = player
        var resumed = false
        fun resumeOnce(result: PlayResult) {
            if (!resumed) {
                resumed = true
                if (cont.isActive) cont.resume(result)
            }
        }

        player.setOnCompletionListener {
            runCatching { it.release() }
            if (activePlayer === player) activePlayer = null
            resumeOnce(PlayResult.Played)
        }
        player.setOnErrorListener { _, what, extra ->
            runCatching { player.release() }
            if (activePlayer === player) activePlayer = null
            resumeOnce(
                PlayResult.Failed(
                    PlayResult.Reason.Playback,
                    IOException("MediaPlayer error what=$what extra=$extra"),
                ),
            )
            true
        }

        cont.invokeOnCancellation {
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
            if (activePlayer === player) activePlayer = null
        }

        try {
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
        } catch (t: Throwable) {
            runCatching { player.release() }
            if (activePlayer === player) activePlayer = null
            resumeOnce(PlayResult.Failed(PlayResult.Reason.Playback, t))
        }
    }

    private fun cacheKey(request: AudioRequest): String {
        val raw = "${request.voice}|${request.text}".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(raw)
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    @Serializable
    private data class TtsRequest(
        @SerialName("text") val text: String,
        @SerialName("voice") val voice: String,
    )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val DEFAULT_TIMEOUT_SEC: Long = 15
    }
}

data class AudioConfig(val baseUrl: String)

internal fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()
