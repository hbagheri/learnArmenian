package com.learnarm.core.data.seed

import android.content.Context
import com.learnarm.core.database.dao.PhraseDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhrasesSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phraseDao: PhraseDao,
) {
    suspend fun seedIfNeeded() {
        if (phraseDao.count() > 0) return
        val raw = context.assets.open(SEED_ASSET).bufferedReader().use { it.readText() }
        val seeds = json.decodeFromString<List<PhraseSeed>>(raw)
        phraseDao.insertAll(seeds.map(PhraseSeed::toEntity))
    }

    private companion object {
        const val SEED_ASSET = "seed/phrases.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
