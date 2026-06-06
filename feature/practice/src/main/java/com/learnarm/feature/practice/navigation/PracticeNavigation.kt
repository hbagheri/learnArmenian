package com.learnarm.feature.practice.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.practice.PracticeScreen
import kotlinx.serialization.Serializable

@Serializable
data object PracticeRoute

fun NavGraphBuilder.practiceScreen(onBack: () -> Unit) {
    composable<PracticeRoute> {
        PracticeScreen(onBack = onBack)
    }
}

fun NavController.navigateToPractice() {
    navigate(PracticeRoute)
}
