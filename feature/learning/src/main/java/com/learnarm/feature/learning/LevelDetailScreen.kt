package com.learnarm.feature.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelDetailScreen(
    onBack: () -> Unit,
    onReviewLesson: (Int) -> Unit,
    onChapterExam: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LevelDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val bestScore by viewModel.bestScore.collectAsStateWithLifecycle()
    var showScore by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    when (val s = uiState) {
                        is LevelDetailUiState.Ready -> s.level.title
                        else -> "فصل"
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                    )
                }
            },
        )

        when (val s = uiState) {
            LevelDetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is LevelDetailUiState.Ready -> ChapterMenu(
                level = s.level,
                bestScore = bestScore,
                showScore = showScore,
                onReviewLesson = { onReviewLesson(viewModel.levelId) },
                onChapterExam = { onChapterExam(viewModel.levelId) },
                onToggleScore = { showScore = !showScore },
            )
        }
    }
}

@Composable
private fun ChapterMenu(
    level: Level,
    bestScore: Int,
    showScore: Boolean,
    onReviewLesson: () -> Unit,
    onChapterExam: () -> Unit,
    onToggleScore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = level.titleFa,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = level.description,
            style = MaterialTheme.typography.bodyLarge,
        )

        Button(
            onClick = onReviewLesson,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = ButtonDefaults.ContentPadding,
        ) {
            Text(
                text = "مرور درس",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Button(
            onClick = onChapterExam,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "آزمون فصل",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        OutlinedButton(
            onClick = onToggleScore,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "امتیاز شما",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        if (showScore) {
            ScoreCard(score = bestScore)
        }
    }
}

@Composable
private fun ScoreCard(score: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (score > 0) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (score > 0) "$score%" else "هنوز آزمون نداده‌اید",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (score > 0) {
                Text(
                    text = "بهترین امتیاز شما",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
