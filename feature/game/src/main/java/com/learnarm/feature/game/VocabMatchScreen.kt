package com.learnarm.feature.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabMatchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TopAppBar(
            title = { Text("بازی تطابق واژگان") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                    )
                }
            },
        )

        when (gameState) {
            GameUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is GameUiState.Playing -> {
                val state = gameState as GameUiState.Playing
                GamePlayingContent(
                    state = state,
                    onSelectArmenian = viewModel::selectArmenian,
                    onMatch = viewModel::matchWord,
                )
            }

            is GameUiState.GameOver -> {
                val state = gameState as GameUiState.GameOver
                GameOverContent(
                    state = state,
                    onRestart = viewModel::restartGame,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun GamePlayingContent(
    state: GameUiState.Playing,
    onSelectArmenian: (Int) -> Unit,
    onMatch: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScoreBar(
            score = state.score,
            total = state.totalWords,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ArmenianColumn(
                wordPairs = state.wordPairs,
                matches = state.matches,
                selectedPosition = state.selectedArmenian,
                onSelect = onSelectArmenian,
                modifier = Modifier.weight(1f),
            )

            PersianColumn(
                persianOptions = state.persianOptions,
                selectedArmenian = state.selectedArmenian,
                matches = state.matches,
                wordPairs = state.wordPairs,
                onMatch = onMatch,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScoreBar(
    score: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val animatedScore by animateIntAsState(targetValue = score, label = "score")
    val progress = if (total > 0) (animatedScore.toFloat() / total) else 0f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "امتیاز",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "$animatedScore / $total",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun ArmenianColumn(
    wordPairs: List<WordPair>,
    matches: List<GameMatch>,
    selectedPosition: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "ارمنی",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(wordPairs.size) { index ->
                val isMatched = matches.any { it.armPosition == index }
                if (!isMatched) {
                    ArmenianWordCard(
                        word = wordPairs[index].armenian,
                        isSelected = selectedPosition == index,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PersianColumn(
    persianOptions: List<String>,
    selectedArmenian: Int?,
    matches: List<GameMatch>,
    wordPairs: List<WordPair>,
    onMatch: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "فارسی",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(persianOptions.size) { index ->
                val isPersianUsed = matches.any {
                    wordPairs[it.armPosition].persian == persianOptions[index]
                }
                if (!isPersianUsed) {
                    PersianWordCard(
                        word = persianOptions[index],
                        isClickable = selectedArmenian != null,
                        onClick = {
                            if (selectedArmenian != null) {
                                onMatch(selectedArmenian, index)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArmenianWordCard(
    word: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.headlineSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            fontSize = 32.sp,
        )
    }
}

@Composable
private fun PersianWordCard(
    word: String,
    isClickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isClickable) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(enabled = isClickable, onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isClickable) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun GameOverContent(
    state: GameUiState.GameOver,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "بازی تمام شد! 🎉",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${state.percentage}%",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Text(
            text = "${state.score} از ${state.totalWords} صحیح",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        val feedback = when (state.percentage) {
            100 -> "عالی! تمام کلمات را صحیح توپ زدی! 🌟"
            in 80..99 -> "خیلی خوب! نزدیک به کمال! 😊"
            in 60..79 -> "خوب! سعی کن بیشتر تمرین کنی 💪"
            else -> "دوباره تلاش کن! 🔄"
        }

        Text(
            text = feedback,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text("دوباره بازی")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("بازگشت")
        }
    }
}
