package com.learnarm.feature.learning.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.learning.LevelsScreen
import com.learnarm.feature.learning.LevelDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data object LearningRoute

@Serializable
data object LevelDetailRoute

fun NavGraphBuilder.learningScreen(
    navController: NavController,
    onPhaseSelected: (Int) -> Unit,
) {
    composable<LearningRoute> {
        LevelsScreen(
            onLevelSelected = { navController.navigateToLevelDetail() },
        )
    }
    composable<LevelDetailRoute> {
        LevelDetailScreen(
            onBack = { navController.popBackStack() },
            onPhaseSelected = onPhaseSelected,
        )
    }
}

fun NavController.navigateToLearning() {
    navigate(LearningRoute)
}

fun NavController.navigateToLevelDetail() {
    navigate(LevelDetailRoute)
}
