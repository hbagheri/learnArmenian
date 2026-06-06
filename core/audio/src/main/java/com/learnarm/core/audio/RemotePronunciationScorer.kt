package com.learnarm.core.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePronunciationScorer @Inject constructor(
    private val httpClient: OkHttpClient,
    private val config: AudioConfig,
) : PronunciationScorer {

    override suspend fun score(audioFile: File, target: String): ScoreResult =
        withContext(Dispatchers.IO) {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext ScoreResult.Failed(ScoreResult.Reason.EmptyAudio)
            }
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        name = "audio",
                        filename = audioFile.name,
                        body = audioFile.asRequestBody(AUDIO_MEDIA_TYPE),
                    )
                    .addFormDataPart(name = "target", value = target)
                    .build()
                val request = Request.Builder()
                    .url(config.baseUrl.trimEnd('/') + "/pronunciation-score")
                    .post(body)
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use ScoreResult.Failed(
                            ScoreResult.Reason.Server,
                            IOException("HTTP ${response.code}"),
                        )
                    }
                    val text = response.body?.string()
                        ?: return@use ScoreResult.Failed(ScoreResult.Reason.Parse)
                    runCatching {
                        Json.decodeFromString(ScoreResponse.serializer(), text)
                    }.fold(
                        onSuccess = { parsed ->
                            ScoreResult.Success(
                                score = parsed.score,
                                recognized = parsed.recognized,
                                feedback = parsed.feedback,
                            )
                        },
                        onFailure = { e ->
                            ScoreResult.Failed(ScoreResult.Reason.Parse, e)
                        },
                    )
                }
            } catch (io: IOException) {
                ScoreResult.Failed(ScoreResult.Reason.Network, io)
            }
        }

    @Serializable
    private data class ScoreResponse(
        @SerialName("target") val target: String,
        @SerialName("recognized") val recognized: String,
        @SerialName("score") val score: Float,
        @SerialName("feedback") val feedback: String,
    )

    private companion object {
        val AUDIO_MEDIA_TYPE = "audio/mp4".toMediaType()

        @Suppress("unused")
        fun emptyFormBody(): okhttp3.RequestBody = "".toRequestBody()
    }
}
