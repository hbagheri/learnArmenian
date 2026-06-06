package com.learnarm.feature.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoriesViewModel = hiltViewModel(),
) {
    val storyState by viewModel.storyReaderState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TopAppBar(
            title = {
                when (storyState) {
                    is StoryReaderUiState.Ready -> {
                        Text((storyState as StoryReaderUiState.Ready).story.titleArmenian)
                    }

                    else -> Text("داستان")
                }
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

        when (storyState) {
            StoryReaderUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is StoryReaderUiState.Ready -> {
                val state = storyState as StoryReaderUiState.Ready
                StoryReaderContent(
                    story = state.story,
                    selectedWordIndex = state.selectedWordIndex,
                    onWordSelected = { index -> viewModel.selectWord(index) },
                    onClearSelection = { viewModel.clearWordSelection() },
                )
            }
        }
    }
}

@Composable
private fun StoryReaderContent(
    story: Story,
    selectedWordIndex: Int?,
    onWordSelected: (Int) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = story.titleArmenian,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = story.titlePersian,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                StoryTextWithVocabulary(
                    story = story,
                    selectedWordIndex = selectedWordIndex,
                    onWordSelected = onWordSelected,
                )
            }

            item {
                VocabularyList(vocabulary = story.vocabulary)
            }
        }

        if (selectedWordIndex != null) {
            VocabularyPopup(
                word = story.vocabulary[selectedWordIndex],
                onDismiss = onClearSelection,
            )
        }
    }
}

@Composable
private fun StoryTextWithVocabulary(
    story: Story,
    selectedWordIndex: Int?,
    onWordSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            StoryText(
                story = story,
                selectedWordIndex = selectedWordIndex,
                onWordSelected = onWordSelected,
            )
        }
    }
}

@Composable
private fun StoryText(
    story: Story,
    selectedWordIndex: Int?,
    onWordSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = story.content.split("\\s+".toRegex())
    val vocabulary = story.vocabulary
    val vocabularyWords = vocabulary.map { it.word }.toSet()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopEnd),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        words.forEachIndexed { index, word ->
            val cleanWord = word.replace(Regex("[։,\\.;\\-]"), "")
            val vocabIndex = vocabulary.indexOfFirst { it.word == cleanWord }
            val isVocab = vocabularyWords.contains(cleanWord)
            val isSelected = index == selectedWordIndex

            if (isVocab && vocabIndex >= 0) {
                Text(
                    text = word,
                    modifier = Modifier
                        .clickable { onWordSelected(vocabIndex) }
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontStyle = FontStyle.Italic,
                    ),
                )
            } else {
                Text(
                    text = word,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun VocabularyList(
    vocabulary: List<StoryWord>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "واژه‌نامه",
            style = MaterialTheme.typography.titleMedium,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vocabulary.forEach { word ->
                VocabularyItem(word = word)
            }
        }
    }
}

@Composable
private fun VocabularyItem(
    word: StoryWord,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = word.word,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.4f),
        )
        Column(
            modifier = Modifier.weight(0.6f),
        ) {
            Text(
                text = word.persianTranslation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            word.pronunciation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VocabularyPopup(
    word: StoryWord,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
            .background(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = word.persianTranslation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                word.pronunciation?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "برای بستن، روی صفحه بزنید",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
