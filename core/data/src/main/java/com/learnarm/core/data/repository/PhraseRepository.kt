package com.learnarm.core.data.repository

import com.learnarm.core.database.dao.PhraseDao
import com.learnarm.core.database.entity.PhraseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhraseRepository @Inject constructor(
    private val phraseDao: PhraseDao,
) {
    fun observePhrases(): Flow<List<PhraseEntity>> = phraseDao.observeAll()

    fun observeByCategory(category: String): Flow<List<PhraseEntity>> =
        phraseDao.observeByCategory(category)

    suspend fun getById(id: Int): PhraseEntity? = phraseDao.getById(id)
}
