package com.learnarm.core.data.streak

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val Context.streakDataStore by preferencesDataStore(name = "streak")

@Singleton
class StreakStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val store get() = context.streakDataStore

    val currentStreak: Flow<Int> = store.data.map { prefs ->
        prefs[KEY_STREAK] ?: 0
    }

    suspend fun recordStudiedToday(today: LocalDate = LocalDate.now(ZoneId.systemDefault())) {
        store.edit { prefs ->
            val lastDay = prefs[KEY_LAST_EPOCH_DAY]
            val todayEpoch = today.toEpochDay()
            if (lastDay == todayEpoch) return@edit
            val streak = prefs[KEY_STREAK] ?: 0
            val nextStreak = when {
                lastDay == null -> 1
                lastDay == todayEpoch - 1 -> streak + 1
                else -> 1
            }
            prefs[KEY_STREAK] = nextStreak
            prefs[KEY_LAST_EPOCH_DAY] = todayEpoch
        }
    }

    private companion object {
        val KEY_STREAK: Preferences.Key<Int> = intPreferencesKey("current_streak")
        val KEY_LAST_EPOCH_DAY: Preferences.Key<Long> = longPreferencesKey("last_epoch_day")
    }
}
