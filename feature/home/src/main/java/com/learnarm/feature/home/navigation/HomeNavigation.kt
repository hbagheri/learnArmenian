package com.learnarm.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onStart: () -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(onStart = onStart)
    }
}
