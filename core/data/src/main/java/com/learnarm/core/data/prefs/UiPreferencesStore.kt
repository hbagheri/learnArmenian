package com.learnarm.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** How the letter tile shows pronunciation under the Armenian glyph. */
enum class PronunciationDisplayMode { Hidden, Phonetic, Persian }

private val Context.uiPrefsDataStore by preferencesDataStore(name = "ui_prefs")

@Singleton
class UiPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.uiPrefsDataStore

    val pronunciationDisplayMode: Flow<PronunciationDisplayMode> = store.data.map { prefs ->
        when (prefs[KEY_PRONUNCIATION_MODE]) {
            "Phonetic" -> PronunciationDisplayMode.Phonetic
            "Persian" -> PronunciationDisplayMode.Persian
            else -> PronunciationDisplayMode.Hidden
        }
    }

    suspend fun setPronunciationDisplayMode(mode: PronunciationDisplayMode) {
        store.edit { prefs -> prefs[KEY_PRONUNCIATION_MODE] = mode.name }
    }

    /** Cycle Hidden → Phonetic → Persian → Hidden. */
    suspend fun cyclePronunciationDisplayMode() {
        store.edit { prefs ->
            val current = when (prefs[KEY_PRONUNCIATION_MODE]) {
                "Phonetic" -> PronunciationDisplayMode.Phonetic
                "Persian" -> PronunciationDisplayMode.Persian
                else -> PronunciationDisplayMode.Hidden
            }
            val next = when (current) {
                PronunciationDisplayMode.Hidden -> PronunciationDisplayMode.Phonetic
                PronunciationDisplayMode.Phonetic -> PronunciationDisplayMode.Persian
                PronunciationDisplayMode.Persian -> PronunciationDisplayMode.Hidden
            }
            prefs[KEY_PRONUNCIATION_MODE] = next.name
        }
    }

    private companion object {
        val KEY_PRONUNCIATION_MODE = stringPreferencesKey("pronunciation_mode")
    }
}
