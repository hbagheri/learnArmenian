package com.learnarm.core.data.seed

import android.content.Context
import com.learnarm.core.database.dao.LetterDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlphabetSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val letterDao: LetterDao,
) {
    suspend fun seedIfNeeded() {
        if (letterDao.count() > 0) return
        val raw = context.assets.open(SEED_ASSET).bufferedReader().use { it.readText() }
        val seeds = json.decodeFromString<List<LetterSeed>>(raw)
        letterDao.insertAll(seeds.map(LetterSeed::toEntity))
    }

    private companion object {
        const val SEED_ASSET = "seed/alphabet.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
