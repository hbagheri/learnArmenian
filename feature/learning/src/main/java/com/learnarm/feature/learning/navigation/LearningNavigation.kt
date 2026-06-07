package com.learnarm.feature.learning.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.learning.LevelDetailScreen
import com.learnarm.feature.learning.LevelsScreen
import com.learnarm.feature.learning.letters.LetterLessonScreen
import com.learnarm.feature.learning.letters.LetterTypingTestScreen
import com.learnarm.feature.learning.vocab.VocabReviewScreen
import kotlinx.serialization.Serializable

@Serializable
data object LearningRoute

@Serializable
data class LevelDetailRoute(val levelId: Int)

@Serializable
data object LetterLessonRoute

@Serializable
data class LetterTypingTestRoute(
    val batchIndex: Int,
    val isReview: Boolean,
    val forReplay: Boolean = false,
)

@Serializable
data object VocabReviewRoute

fun NavGraphBuilder.learningScreen(
    navController: NavController,
    onReviewLesson: (Int) -> Unit,
    onChapterExam: (Int) -> Unit,
) {
    composable<LearningRoute> {
        LevelsScreen(
            onLevelSelected = { levelId -> navController.navigateToLevelDetail(levelId) },
        )
    }
    composable<LevelDetailRoute> {
        LevelDetailScreen(
            onBack = { navController.popBackStack() },
            onReviewLesson = onReviewLesson,
            onChapterExam = onChapterExam,
        )
    }
    composable<LetterLessonRoute> {
        LetterLessonScreen(
            onBack = { navController.popBackStack() },
            onStartBatchTest = { batch, forReplay ->
                navController.navigateToLetterTypingTest(
                    batchIndex = batch,
                    isReview = false,
                    forReplay = forReplay,
                )
            },
            onStartReview = { _, batch ->
                navController.navigateToLetterTypingTest(batchIndex = batch, isReview = true)
            },
            onGoToFinalExam = {
                // Pop back to LevelDetail; user can tap "آزمون فصل" from there to enter the matching game.
                navController.popBackStack()
            },
        )
    }
    composable<LetterTypingTestRoute> {
        LetterTypingTestScreen(
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack() },
        )
    }
    composable<VocabReviewRoute> {
        VocabReviewScreen(
            onBack = { navController.popBackStack() },
        )
    }
}

fun NavController.navigateToLearning() {
    navigate(LearningRoute)
}

fun NavController.navigateToLevelDetail(levelId: Int) {
    navigate(LevelDetailRoute(levelId))
}

fun NavController.navigateToLetterLesson() {
    navigate(LetterLessonRoute)
}

fun NavController.navigateToLetterTypingTest(
    batchIndex: Int,
    isReview: Boolean,
    forReplay: Boolean = false,
) {
    navigate(
        LetterTypingTestRoute(
            batchIndex = batchIndex,
            isReview = isReview,
            forReplay = forReplay,
        ),
    )
}

fun NavController.navigateToVocabReview() {
    navigate(VocabReviewRoute)
}
