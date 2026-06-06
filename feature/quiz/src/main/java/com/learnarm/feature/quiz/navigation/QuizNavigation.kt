package com.learnarm.feature.quiz.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.quiz.QuizScreen
import kotlinx.serialization.Serializable

@Serializable
data object QuizRoute

fun NavGraphBuilder.quizScreen(onBack: () -> Unit) {
    composable<QuizRoute> {
        QuizScreen(onBack = onBack)
    }
}

fun NavController.navigateToQuiz() {
    navigate(QuizRoute)
}
