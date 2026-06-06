package com.learnarm.feature.phrases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnarm.core.audio.PlayResult
import com.learnarm.core.database.entity.PhraseEntity
import com.learnarm.core.designsystem.theme.LearnArmTheme

@Composable
fun PhrasesScreen(
    modifier: Modifier = Modifier,
    viewModel: PhrasesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PhrasesContent(
        state = state,
        onCategorySelected = viewModel::onCategorySelected,
        onPhraseTap = viewModel::onPhraseTapped,
        modifier = modifier,
    )
}

@Composable
private fun PhrasesContent(
    state: PhrasesUiState,
    onCategorySelected: (PhraseCategory) -> Unit,
    onPhraseTap: (PhraseEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "عبارات",
            style = MaterialTheme.typography.titleLarge,
        )

        when (state) {
            PhrasesUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is PhrasesUiState.Ready -> {
                CategoryTabs(
                    selected = state.selected,
                    onCategorySelected = onCategorySelected,
                )
                state.playback.lastError?.let { reason ->
                    Text(
                        text = errorMessage(reason),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                PhrasesList(
                    phrases = state.phrases,
                    playingPhraseId = state.playback.playingPhraseId,
                    onPhraseTap = onPhraseTap,
                )
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selected: PhraseCategory,
    onCategorySelected: (PhraseCategory) -> Unit,
) {
    val tabs = PhraseCategory.ordered
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        tabs.forEachIndexed { index, category ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onCategorySelected(category) },
                text = { Text(category.labelFa) },
            )
        }
    }
}

@Composable
private fun PhrasesList(
    phrases: List<PhraseEntity>,
    playingPhraseId: Int?,
    onPhraseTap: (PhraseEntity) -> Unit,
) {
    if (phrases.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "هنوز عبارتی در این دسته نیست.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(phrases, key = { it.id }) { phrase ->
            PhraseCard(
                phrase = phrase,
                isPlaying = phrase.id == playingPhraseId,
                onClick = { onPhraseTap(phrase) },
            )
        }
    }
}

@Composable
private fun PhraseCard(
    phrase: PhraseEntity,
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
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = phrase.armenian,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = phrase.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = phrase.persian,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                phrase.note?.takeIf { it.isNotBlank() }?.let { noteText ->
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (isPlaying) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(16.dp),
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

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PhrasesContentPreview() {
    LearnArmTheme {
        PhrasesContent(
            state = PhrasesUiState.Ready(
                selected = PhraseCategory.Greeting,
                phrases = listOf(
                    PhraseEntity(
                        id = 1, orderIndex = 1, category = "greeting",
                        armenian = "Բարև", transliteration = "barev",
                        persian = "سلام", note = "محاوره‌ای",
                    ),
                    PhraseEntity(
                        id = 4, orderIndex = 4, category = "greeting",
                        armenian = "Բարի լույս", transliteration = "bari luys",
                        persian = "صبح بخیر", note = null,
                    ),
                ),
                playback = PhrasePlayback(playingPhraseId = 1),
            ),
            onCategorySelected = {},
            onPhraseTap = {},
        )
    }
}
