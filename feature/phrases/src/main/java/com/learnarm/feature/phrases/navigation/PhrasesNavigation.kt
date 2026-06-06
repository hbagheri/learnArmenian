package com.learnarm.feature.phrases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.phrases.PhrasesScreen
import kotlinx.serialization.Serializable

@Serializable
data object PhrasesRoute

fun NavGraphBuilder.phrasesScreen() {
    composable<PhrasesRoute> {
        PhrasesScreen()
    }
}

fun NavController.navigateToPhrases() {
    navigate(PhrasesRoute)
}
