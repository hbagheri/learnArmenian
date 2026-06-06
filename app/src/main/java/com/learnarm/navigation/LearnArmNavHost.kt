package com.learnarm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.learnarm.feature.home.navigation.HomeRoute
import com.learnarm.feature.home.navigation.homeScreen
import com.learnarm.feature.lessons.LessonsSharedViewModel
import com.learnarm.feature.lessons.navigation.lessonsScreen
import com.learnarm.feature.lessons.navigation.lessonRunnerScreen
import com.learnarm.feature.lessons.navigation.navigateToLessons
import com.learnarm.feature.lessons.navigation.navigateToLessonRunner
import androidx.hilt.navigation.compose.hiltViewModel
import com.learnarm.feature.phrases.navigation.navigateToPhrases
import com.learnarm.feature.phrases.navigation.phrasesScreen
import com.learnarm.feature.practice.navigation.navigateToPractice
import com.learnarm.feature.practice.navigation.practiceScreen
import com.learnarm.feature.quiz.navigation.navigateToQuiz
import com.learnarm.feature.quiz.navigation.quizScreen
import com.learnarm.feature.reviews.navigation.navigateToReviews
import com.learnarm.feature.reviews.navigation.reviewsScreen
import com.learnarm.feature.stories.navigation.navigateToStories
import com.learnarm.feature.stories.navigation.navigateToStoryReader
import com.learnarm.feature.stories.navigation.storiesScreen

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
            onStartQuiz = { navController.navigateToQuiz() },
            onOpenPhrases = { navController.navigateToPhrases() },
            onStartPractice = { navController.navigateToPractice() },
            onOpenReviews = { navController.navigateToReviews() },
            onOpenStories = { navController.navigateToStories() },
            onContinueLesson = { navController.navigateToLessons() },
        )
        quizScreen(onBack = { navController.popBackStack() })
        phrasesScreen()
        practiceScreen(onBack = { navController.popBackStack() })
        reviewsScreen(onBack = { navController.popBackStack() })
        storiesScreen(
            onBack = { navController.popBackStack() },
            onStorySelected = { navController.navigateToStoryReader() },
        )
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
