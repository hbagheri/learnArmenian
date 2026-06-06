package com.learnarm.feature.stories

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class StoryWord(
    val word: String,
    val persianTranslation: String,
    val pronunciation: String? = null,
)

data class Story(
    val id: Int,
    val titleArmenian: String,
    val titlePersian: String,
    val content: String,
    val difficulty: String, // easy, medium, hard
    val vocabulary: List<StoryWord>,
)

sealed class StoriesUiState {
    object Loading : StoriesUiState()
    data class Ready(
        val stories: List<Story>,
    ) : StoriesUiState()
    data class Error(val message: String) : StoriesUiState()
}

sealed class StoryReaderUiState {
    object Loading : StoryReaderUiState()
    data class Ready(
        val story: Story,
        val selectedWordIndex: Int? = null,
    ) : StoryReaderUiState()
}

@HiltViewModel
class StoriesViewModel @Inject constructor() : ViewModel() {
    private val _storiesUiState = MutableStateFlow<StoriesUiState>(StoriesUiState.Loading)
    val storiesUiState: StateFlow<StoriesUiState> = _storiesUiState

    private val _storyReaderState = MutableStateFlow<StoryReaderUiState>(StoryReaderUiState.Loading)
    val storyReaderState: StateFlow<StoryReaderUiState> = _storyReaderState

    init {
        loadStories()
    }

    private fun loadStories() {
        val sampleStories = listOf(
            Story(
                id = 1,
                titleArmenian = "Առաջին օր",
                titlePersian = "روز اول",
                content = "Մի անգամ մի պატանի կար։ Նա շատ ուրախ էր։ Ամեն օր նա դեղձ կերավ։ Նա իր հայրիկին ասաց: «Հայրիկ, դեղձ ուտենք։»",
                difficulty = "easy",
                vocabulary = listOf(
                    StoryWord("պատանի", "پسر"),
                    StoryWord("ուրախ", "خوشحال"),
                    StoryWord("ամեն օր", "هر روز"),
                    StoryWord("դեղձ", "هلو"),
                    StoryWord("հայրիկ", "پدر"),
                    StoryWord("ուտենք", "بخوریم"),
                ),
            ),
            Story(
                id = 2,
                titleArmenian = "Թռչուններ",
                titlePersian = "پرندگان",
                content = "Մի լեռ կար։ Լեռի վրա շատ թռչուններ ապրում էին։ Նրանք ուժեղ և արագ չէին, այլ խելացի էին։ Ամեն առավոտ նրանք երգում էին գեղեցիկ երգ։",
                difficulty = "medium",
                vocabulary = listOf(
                    StoryWord("լեռ", "کوه"),
                    StoryWord("վրա", "روی"),
                    StoryWord("թռչուն", "پرنده"),
                    StoryWord("ապրել", "زندگی کردن"),
                    StoryWord("ուժեղ", "قوی"),
                    StoryWord("խելացի", "هوشمند"),
                    StoryWord("առավոտ", "صبح"),
                    StoryWord("երգել", "آواز خواندن"),
                ),
            ),
        )
        _storiesUiState.value = StoriesUiState.Ready(sampleStories)
    }

    fun selectStory(story: Story) {
        _storyReaderState.value = StoryReaderUiState.Ready(story)
    }

    fun selectWord(index: Int) {
        val state = _storyReaderState.value
        if (state is StoryReaderUiState.Ready) {
            _storyReaderState.value = state.copy(
                selectedWordIndex = if (state.selectedWordIndex == index) null else index
            )
        }
    }

    fun clearWordSelection() {
        val state = _storyReaderState.value
        if (state is StoryReaderUiState.Ready) {
            _storyReaderState.value = state.copy(selectedWordIndex = null)
        }
    }
}
