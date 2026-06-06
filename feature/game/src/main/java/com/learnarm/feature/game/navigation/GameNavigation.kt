package com.learnarm.feature.game.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.game.VocabMatchScreen
import kotlinx.serialization.Serializable

@Serializable
data object GameRoute

fun NavGraphBuilder.gameScreen(onBack: () -> Unit) {
    composable<GameRoute> {
        VocabMatchScreen(onBack = onBack)
    }
}

fun NavController.navigateToGame() {
    navigate(GameRoute)
}
