package com.learnarm.feature.game.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.game.VocabMatchScreen
import kotlinx.serialization.Serializable

object GameTypes {
    const val LETTERS = "letters"
    const val VOCABULARY = "vocabulary"
    const val SENTENCES = "sentences"
}

@Serializable
data class GameRoute(val gameType: String = GameTypes.LETTERS)

fun NavGraphBuilder.gameScreen(onBack: () -> Unit) {
    composable<GameRoute> {
        VocabMatchScreen(onBack = onBack)
    }
}

fun NavController.navigateToGame(gameType: String = GameTypes.LETTERS) {
    navigate(GameRoute(gameType = gameType))
}
