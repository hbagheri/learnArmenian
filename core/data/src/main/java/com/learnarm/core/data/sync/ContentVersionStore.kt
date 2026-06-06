package com.learnarm.core.data.sync

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.contentVersionDataStore by preferencesDataStore(name = "content_version")

@Singleton
class ContentVersionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getCurrentVersion(): Int =
        context.contentVersionDataStore.data.first()[KEY] ?: 0

    suspend fun setCurrentVersion(version: Int) {
        context.contentVersionDataStore.edit { it[KEY] = version }
    }

    private companion object {
        val KEY: Preferences.Key<Int> = intPreferencesKey("lessons_version")
    }
}
