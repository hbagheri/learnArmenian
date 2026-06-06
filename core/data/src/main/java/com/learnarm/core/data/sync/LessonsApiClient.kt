package com.learnarm.core.data.sync

import com.learnarm.core.data.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonsApiClient @Inject constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun fetchVersion(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(baseUrl + "/content/version").get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                json.decodeFromString(VersionDto.serializer(), body).version
            }
        }.getOrNull()
    }

    suspend fun fetchLessons(): LessonsResponseDto? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(baseUrl + "/content/lessons").get().build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw IOException("empty body")
                json.decodeFromString(LessonsResponseDto.serializer(), body)
            }
        }.getOrNull()
    }

    private val baseUrl: String
        get() = BuildConfig.LEARNARM_API_BASE_URL.trimEnd('/')

    @kotlinx.serialization.Serializable
    private data class VersionDto(val version: Int)
}
