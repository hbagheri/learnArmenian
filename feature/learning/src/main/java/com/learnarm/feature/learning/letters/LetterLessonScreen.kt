package com.learnarm.feature.learning.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.learnarm.core.data.api.BatchWordDto
import com.learnarm.core.database.entity.LetterEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterLessonScreen(
    onBack: () -> Unit,
    onStartBatchTest: (batchIndex: Int, forReplay: Boolean) -> Unit,
    onStartReview: (roundIndex: Int, batchIndex: Int) -> Unit,
    onGoToFinalExam: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LetterLessonViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chips by viewModel.chips.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("مرور درس — حروف") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                    )
                }
            },
        )

        if (chips.isNotEmpty()) {
            BatchChipsRow(
                chips = chips,
                onChipTap = { chip ->
                    when (chip.status) {
                        BatchChipStatus.Passed -> viewModel.startReplay(chip.index)
                        BatchChipStatus.Current, BatchChipStatus.Replaying ->
                            viewModel.exitReplay()
                        BatchChipStatus.Locked -> Unit
                    }
                },
            )
        }

        when (val s = state) {
            LetterLessonUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is LetterLessonUiState.BatchLesson -> BatchLessonContent(
                state = s,
                onPlayLetter = viewModel::playLetterAudio,
                onPlayWord = viewModel::playWordAudio,
                onStartTest = { onStartBatchTest(s.batchIndex, s.isReplay) },
                onExitReplay = viewModel::exitReplay,
            )

            is LetterLessonUiState.ReviewPending -> ReviewPendingContent(
                state = s,
                onStart = { onStartReview(s.roundIndex, s.justFinishedBatch) },
            )

            LetterLessonUiState.AllBatchesCompleted -> AllCompletedContent(
                onGoToFinalExam = onGoToFinalExam,
            )
        }
    }
}

@Composable
private fun BatchChipsRow(
    chips: List<BatchChip>,
    onChipTap: (BatchChip) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = chips, key = { it.index }) { chip ->
            BatchChipItem(chip = chip, onTap = { onChipTap(chip) })
        }
    }
}

@Composable
private fun BatchChipItem(chip: BatchChip, onTap: () -> Unit) {
    val (bg, fg) = when (chip.status) {
        BatchChipStatus.Passed ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        BatchChipStatus.Current ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        BatchChipStatus.Replaying ->
            MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        BatchChipStatus.Locked ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val tappable = chip.status != BatchChipStatus.Locked
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .let { if (tappable) it.clickable(onClick = onTap) else it },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (chip.status) {
                BatchChipStatus.Passed -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                }
                BatchChipStatus.Locked -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                }
                else -> Unit
            }
            Text(
                text = chip.index.toString(),
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (chip.status == BatchChipStatus.Current || chip.status == BatchChipStatus.Replaying) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
            )
        }
    }
}

@Composable
private fun BatchLessonContent(
    state: LetterLessonUiState.BatchLesson,
    onPlayLetter: (LetterEntity) -> Unit,
    onPlayWord: (BatchWordDto) -> Unit,
    onStartTest: () -> Unit,
    onExitReplay: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isReplay) {
            item { ReplayBanner(batchIndex = state.batchIndex, onExitReplay = onExitReplay) }
        } else {
            item {
                ProgressHeader(state.batchIndex, state.totalBatches, state.passedCount)
            }
        }
        item {
            Text(
                text = "حروف این بسته (روی هرکدوم بزن تا صدا بشنوی)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            BatchLettersRow(
                letters = state.batchLetters,
                playingKey = state.playingKey,
                onTap = onPlayLetter,
            )
        }

        item {
            TargetWordCard(
                word = state.batch.targetWord,
                isPlaying = state.playingKey == "word-${state.batch.targetWord.armenian}",
                onPlay = { onPlayWord(state.batch.targetWord) },
            )
        }

        item {
            Text(
                text = "حالا این کلمات رو می‌تونی بخونی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(
            items = state.batch.readableWords,
            key = { w -> "${state.batchIndex}-${w.armenian}" },
        ) { w ->
            WordRow(
                word = w,
                isPlaying = state.playingKey == "word-${w.armenian}",
                isTarget = w.armenian == state.batch.targetWord.armenian,
                onPlay = { onPlayWord(w) },
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartTest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isReplay) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                Text(
                    text = if (state.isReplay) {
                        "آزمون تمرینی این بسته"
                    } else {
                        "آزمون این بسته (۱۰۰٪ لازمه)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ReplayBanner(batchIndex: Int, onExitReplay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "حالت مرور — بستهٔ $batchIndex",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "روی پیشرفتت اثر نداره. هر وقت خواستی برگرد به بستهٔ فعلی.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Button(
                onClick = onExitReplay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Text(
                    text = "بازگشت",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ProgressHeader(batchIndex: Int, total: Int, passed: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "بستهٔ $batchIndex از $total",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "گذراندی: $passed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { passed.toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BatchLettersRow(
    letters: List<LetterEntity>,
    playingKey: String?,
    onTap: (LetterEntity) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        letters.forEach { letter ->
            LetterTile(
                letter = letter,
                isPlaying = playingKey == "letter-${letter.id}",
                onTap = { onTap(letter) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LetterTile(
    letter: LetterEntity,
    isPlaying: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onTap),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${letter.upper} ${letter.lower}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
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
                        .padding(4.dp)
                        .size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun TargetWordCard(word: BatchWordDto, isPlaying: Boolean, onPlay: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "کلمهٔ هدف",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = word.armenian,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "${word.transliteration}  /  ${word.persian}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaying) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "شنیدن تلفظ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun WordRow(word: BatchWordDto, isPlaying: Boolean, isTarget: Boolean, onPlay: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPlaying -> MaterialTheme.colorScheme.secondaryContainer
                isTarget -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.armenian,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.SemiBold,
                )
                Text(
                    text = "${word.transliteration}  /  ${word.persian}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isPlaying) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "شنیدن")
            }
        }
    }
}

@Composable
private fun ReviewPendingContent(state: LetterLessonUiState.ReviewPending, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "🎯",
            fontSize = 64.sp,
        )
        Text(
            text = "آزمون دور ${state.roundIndex}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "تا اینجا ${state.cumulativeLetterIds.size} حرف یاد گرفتی. " +
                "قبل از باز شدن بستهٔ بعدی باید همهٔ این حروف رو ۱۰۰٪ درست تایپ کنی.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "شروع آزمون دوره",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AllCompletedContent(onGoToFinalExam: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = "🎉", fontSize = 72.sp)
        Text(
            text = "همهٔ بسته‌ها رو گذراندی!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "حالا برو آزمون نهایی فصل (بازی تطابق روی هر ۳۹ حرف) تا فصل بعد باز بشه.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onGoToFinalExam,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "آزمون نهایی فصل",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

