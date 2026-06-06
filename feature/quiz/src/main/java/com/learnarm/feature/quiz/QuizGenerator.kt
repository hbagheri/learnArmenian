package com.learnarm.feature.quiz

import com.learnarm.core.database.entity.LetterEntity
import kotlin.random.Random

data class QuizQuestion(
    val letter: LetterEntity,
    val options: List<String>,
    val correctIndex: Int,
)

object QuizGenerator {

    const val OPTIONS_PER_QUESTION = 4

    fun generateRound(
        letters: List<LetterEntity>,
        questionCount: Int,
        random: Random = Random.Default,
    ): List<QuizQuestion> {
        if (letters.size < OPTIONS_PER_QUESTION) return emptyList()

        val targets = letters.shuffled(random).take(questionCount.coerceAtMost(letters.size))
        return targets.map { target -> buildQuestion(target, letters, random) }
    }

    private fun buildQuestion(
        target: LetterEntity,
        pool: List<LetterEntity>,
        random: Random,
    ): QuizQuestion {
        val distractors = pool
            .asSequence()
            .filter { it.id != target.id && it.pronunciationFa != target.pronunciationFa }
            .map { it.pronunciationFa }
            .distinct()
            .toList()
            .shuffled(random)
            .take(OPTIONS_PER_QUESTION - 1)

        val options = (distractors + target.pronunciationFa).shuffled(random)
        return QuizQuestion(
            letter = target,
            options = options,
            correctIndex = options.indexOf(target.pronunciationFa),
        )
    }
}
