package com.learnarm.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnarm.core.audio.PlayResult
import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.core.designsystem.theme.LearnArmTheme

@Composable
fun HomeScreen(
    onStartQuiz: () -> Unit,
    onOpenPhrases: () -> Unit,
    onStartPractice: () -> Unit,
    onOpenReviews: () -> Unit,
    onOpenStories: () -> Unit,
    onContinueLesson: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = uiState,
        onLetterTap = viewModel::onLetterTapped,
        onStartQuiz = onStartQuiz,
        onOpenPhrases = onOpenPhrases,
        onStartPractice = onStartPractice,
        onOpenReviews = onOpenReviews,
        onOpenStories = onOpenStories,
        onContinueLesson = onContinueLesson,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onLetterTap: (LetterEntity) -> Unit,
    onStartQuiz: () -> Unit,
    onOpenPhrases: () -> Unit,
    onStartPractice: () -> Unit,
    onOpenReviews: () -> Unit,
    onOpenStories: () -> Unit,
    onContinueLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val streak = (state as? HomeUiState.Ready)?.streak ?: 0
    val dueCount = (state as? HomeUiState.Ready)?.dueCount ?: 0
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "الفبای ارمنی",
                style = MaterialTheme.typography.titleLarge,
            )
            if (streak > 0) {
                Text(
                    text = "روزانه: $streak",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Button(onClick = onContinueLesson, modifier = Modifier.fillMaxWidth()) {
            Text("ادامه‌ی درس")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = onOpenPhrases, modifier = Modifier.weight(1f)) {
                Text("عبارات")
            }
            OutlinedButton(onClick = onStartPractice, modifier = Modifier.weight(1f)) {
                Text("تمرین")
            }
            OutlinedButton(onClick = onOpenReviews, modifier = Modifier.weight(1f)) {
                Text(if (dueCount > 0) "مرور ($dueCount)" else "مرور")
            }
            Button(onClick = onStartQuiz, modifier = Modifier.weight(1f)) {
                Text("آزمون")
            }
        }
        OutlinedButton(onClick = onOpenStories, modifier = Modifier.fillMaxWidth()) {
            Text("داستان‌های کوتاه")
        }

        when (state) {
            HomeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is HomeUiState.Ready -> {
                Text(
                    text = "${state.letters.size} حرف — برای شنیدن، روی هر حرف بزن",
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.playback.lastError?.let { reason ->
                    Text(
                        text = errorMessage(reason),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                AlphabetGrid(
                    letters = state.letters,
                    playingLetterId = state.playback.playingLetterId,
                    onLetterTap = onLetterTap,
                )
            }
        }
    }
}

@Composable
private fun AlphabetGrid(
    letters: List<LetterEntity>,
    playingLetterId: Int?,
    onLetterTap: (LetterEntity) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(letters, key = { it.id }) { letter ->
            LetterCard(
                letter = letter,
                isPlaying = letter.id == playingLetterId,
                onClick = { onLetterTap(letter) },
            )
        }
    }
}

@Composable
private fun LetterCard(
    letter: LetterEntity,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val colors = if (isPlaying) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${letter.upper} ${letter.lower}",
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = letter.nameLatin,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = letter.pronunciationFa,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
            if (isPlaying) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(14.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
            }
        }
    }
}

private fun errorMessage(reason: PlayResult.Reason): String = when (reason) {
    PlayResult.Reason.Network -> "خطای شبکه — لطفاً اتصال را بررسی کنید"
    PlayResult.Reason.Server -> "سرور پاسخ‌گو نیست"
    PlayResult.Reason.Decode -> "صدای دریافتی قابل پخش نیست"
    PlayResult.Reason.Playback -> "خطا در پخش صدا"
    PlayResult.Reason.Cancelled -> ""
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    LearnArmTheme {
        HomeContent(
            state = HomeUiState.Ready(
                letters = listOf(
                    LetterEntity(1, 1, "Ա", "ա", "այբ", "ayb", "آ", "/a/", "արև", "arev", "خورشید"),
                    LetterEntity(2, 2, "Բ", "բ", "բեն", "ben", "ب", "/b/", "բարև", "barev", "سلام"),
                ),
                playback = PlaybackState(playingLetterId = 1),
            ),
            onLetterTap = {},
            onStartQuiz = {},
            onOpenPhrases = {},
            onStartPractice = {},
            onOpenReviews = {},
            onContinueLesson = {},
        )
    }
}
