package com.learnarm.core.audio

interface LetterAudioPlayer {

    suspend fun play(request: AudioRequest): PlayResult

    fun stop()
}

data class AudioRequest(
    val key: String,
    val text: String,
    val voice: String = "hy-default",
)

sealed interface PlayResult {
    data object Played : PlayResult
    data class Failed(val reason: Reason, val cause: Throwable? = null) : PlayResult

    enum class Reason { Network, Server, Decode, Playback, Cancelled }
}
