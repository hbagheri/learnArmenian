package com.learnarm.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onStartQuiz: () -> Unit,
    onOpenPhrases: () -> Unit,
    onStartPractice: () -> Unit,
    onOpenReviews: () -> Unit,
    onOpenStories: () -> Unit,
    onStartLearning: () -> Unit,
    onContinueLesson: () -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(
            onStartQuiz = onStartQuiz,
            onOpenPhrases = onOpenPhrases,
            onStartPractice = onStartPractice,
            onOpenReviews = onOpenReviews,
            onOpenStories = onOpenStories,
            onStartLearning = onStartLearning,
            onContinueLesson = onContinueLesson,
        )
    }
}
