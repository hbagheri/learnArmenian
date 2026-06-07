package com.learnarm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.learnarm.feature.game.navigation.GameTypes
import com.learnarm.feature.game.navigation.gameScreen
import com.learnarm.feature.game.navigation.navigateToGame
import com.learnarm.feature.home.navigation.HomeRoute
import com.learnarm.feature.home.navigation.homeScreen
import com.learnarm.feature.learning.navigation.learningScreen
import com.learnarm.feature.learning.navigation.navigateToLearning
import com.learnarm.feature.learning.navigation.navigateToLetterLesson
import com.learnarm.feature.learning.navigation.navigateToVocabReview
import com.learnarm.feature.lessons.LessonsSharedViewModel
import com.learnarm.feature.lessons.navigation.lessonRunnerScreen
import com.learnarm.feature.lessons.navigation.lessonsScreen
import com.learnarm.feature.lessons.navigation.navigateToLessonRunner
import com.learnarm.feature.lessons.navigation.navigateToLessons

@Composable
fun LearnArmNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val lessonsSharedViewModel: LessonsSharedViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(
            onStart = { navController.navigateToLearning() },
        )

        learningScreen(
            navController = navController,
            onReviewLesson = { levelId ->
                when (levelId) {
                    1 -> navController.navigateToLetterLesson()
                    2 -> navController.navigateToVocabReview()
                    // Levels 3-4: review not yet implemented
                }
            },
            onChapterExam = { levelId ->
                when (levelId) {
                    1 -> navController.navigateToGame(GameTypes.LETTERS)
                    2 -> navController.navigateToGame(GameTypes.VOCABULARY)
                    3 -> navController.navigateToGame(GameTypes.SENTENCES)
                    // Level 4: media exam not yet implemented
                }
            },
        )

        gameScreen(onBack = { navController.popBackStack() })

        lessonsScreen(
            sharedViewModel = lessonsSharedViewModel,
            onLessonSelected = { lessonId ->
                lessonsSharedViewModel.setSelectedLessonId(lessonId)
                navController.navigateToLessonRunner()
            },
            onBack = { navController.popBackStack() },
        )
        lessonRunnerScreen(
            sharedViewModel = lessonsSharedViewModel,
            onStepCompleted = { navController.navigateToLessonRunner() },
            onBack = { navController.popBackStack() },
        )
    }
}
