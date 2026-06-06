package com.learnarm.core.audio

import java.io.File

interface PronunciationScorer {
    suspend fun score(audioFile: File, target: String): ScoreResult
}

sealed interface ScoreResult {
    data class Success(
        val score: Float,
        val recognized: String,
        val feedback: String,
    ) : ScoreResult

    data class Failed(val reason: Reason, val cause: Throwable? = null) : ScoreResult

    enum class Reason { Network, Server, EmptyAudio, Parse }
}
