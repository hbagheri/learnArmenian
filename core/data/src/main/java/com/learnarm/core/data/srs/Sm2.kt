package com.learnarm.core.data.srs

enum class ReviewQuality(val score: Int) {
    Again(0),
    Hard(3),
    Good(4),
    Easy(5),
}

data class Sm2State(
    val ease: Float,
    val intervalDays: Int,
    val repetitions: Int,
)

/**
 * Simplified SM-2 (Anki-style). All inputs/outputs are plain values; storage,
 * clocks, and IO live elsewhere.
 */
object Sm2 {

    const val INITIAL_EASE = 2.5f
    const val MIN_EASE = 1.3f

    fun initialState(): Sm2State = Sm2State(
        ease = INITIAL_EASE,
        intervalDays = 0,
        repetitions = 0,
    )

    fun step(current: Sm2State, quality: ReviewQuality): Sm2State {
        val q = quality.score
        if (q < 3) {
            return current.copy(
                repetitions = 0,
                intervalDays = 1,
                ease = (current.ease + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)))
                    .coerceAtLeast(MIN_EASE),
            )
        }
        val nextReps = current.repetitions + 1
        val baseInterval = when (nextReps) {
            1 -> 1
            2 -> 6
            else -> (current.intervalDays * current.ease).toInt().coerceAtLeast(1)
        }
        val interval = when (quality) {
            ReviewQuality.Hard -> (baseInterval * 0.8f).toInt().coerceAtLeast(1)
            ReviewQuality.Easy -> (baseInterval * 1.3f).toInt().coerceAtLeast(1)
            else -> baseInterval
        }
        val updatedEase = (current.ease + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)))
            .coerceAtLeast(MIN_EASE)
        return Sm2State(
            ease = updatedEase,
            intervalDays = interval,
            repetitions = nextReps,
        )
    }
}
