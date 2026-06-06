package com.learnarm.core.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import com.learnarm.core.data.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PhaseDto(
    val id: Int,
    val title: String,
    val description: String,
    val type: String,
    val isCompleted: Boolean = false,
    val score: Int? = null,
)

@Serializable
data class LevelDto(
    val id: Int,
    val title: String,
    val titleFa: String,
    val description: String,
    val phases: List<PhaseDto>,
    val isUnlocked: Boolean,
)

@Singleton
class CurriculumService @Inject constructor() {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val baseUrl: String
        get() = BuildConfig.LEARNARM_API_BASE_URL.trimEnd('/')

    suspend fun getLevels(): List<LevelDto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/levels")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("empty body")
                json.decodeFromString<List<LevelDto>>(body)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getLevel(levelId: Int): LevelDto = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/levels/$levelId")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("empty body")
                json.decodeFromString<LevelDto>(body)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getLevelPhases(levelId: Int): List<PhaseDto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/levels/$levelId/phases")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("empty body")
                json.decodeFromString<List<PhaseDto>>(body)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getPhase(phaseId: Int): PhaseDto = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/phases/$phaseId")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("empty body")
                json.decodeFromString<PhaseDto>(body)
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
