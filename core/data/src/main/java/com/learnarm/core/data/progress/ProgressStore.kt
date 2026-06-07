package com.learnarm.core.data.progress

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.progressDataStore by preferencesDataStore(name = "progress")

@Singleton
class ProgressStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.progressDataStore

    val lettersScore: Flow<Int> = store.data.map { it[KEY_LETTERS_SCORE] ?: 0 }
    val vocabularyScore: Flow<Int> = store.data.map { it[KEY_VOCAB_SCORE] ?: 0 }
    val sentencesScore: Flow<Int> = store.data.map { it[KEY_SENTENCES_SCORE] ?: 0 }

    /** Highest batch the learner has passed (0 = none). Next playable batch is this + 1. */
    val lettersCurrentBatch: Flow<Int> = store.data.map { it[KEY_LETTERS_CURRENT_BATCH] ?: 0 }

    /** Number of periodic-review rounds the learner has cleared (0..2). Gates the next round's first batch. */
    val lettersReviewsPassed: Flow<Int> = store.data.map { it[KEY_LETTERS_REVIEWS_PASSED] ?: 0 }

    suspend fun recordLettersScore(score: Int) = recordBest(KEY_LETTERS_SCORE, score)
    suspend fun recordVocabularyScore(score: Int) = recordBest(KEY_VOCAB_SCORE, score)
    suspend fun recordSentencesScore(score: Int) = recordBest(KEY_SENTENCES_SCORE, score)

    /** Records that the learner passed batch [batchIndex]. Only advances; never moves backwards. */
    suspend fun recordLettersBatchPassed(batchIndex: Int) =
        recordIfHigher(KEY_LETTERS_CURRENT_BATCH, batchIndex)

    /** Records that the learner passed periodic review [roundIndex] (1 or 2). Only advances. */
    suspend fun recordLettersReviewPassed(roundIndex: Int) =
        recordIfHigher(KEY_LETTERS_REVIEWS_PASSED, roundIndex)

    private suspend fun recordIfHigher(key: Preferences.Key<Int>, value: Int) {
        store.edit { prefs ->
            val current = prefs[key] ?: 0
            if (value > current) prefs[key] = value
        }
    }

    private suspend fun recordBest(key: Preferences.Key<Int>, score: Int) {
        store.edit { prefs ->
            val current = prefs[key] ?: 0
            if (score > current) prefs[key] = score
        }
    }

    private companion object {
        val KEY_LETTERS_SCORE: Preferences.Key<Int> = intPreferencesKey("letters_max_score")
        val KEY_VOCAB_SCORE: Preferences.Key<Int> = intPreferencesKey("vocabulary_max_score")
        val KEY_SENTENCES_SCORE: Preferences.Key<Int> = intPreferencesKey("sentences_max_score")
        val KEY_LETTERS_CURRENT_BATCH: Preferences.Key<Int> = intPreferencesKey("letters_current_batch")
        val KEY_LETTERS_REVIEWS_PASSED: Preferences.Key<Int> = intPreferencesKey("letters_reviews_passed")
    }
}
