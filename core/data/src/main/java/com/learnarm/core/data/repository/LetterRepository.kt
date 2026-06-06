package com.learnarm.core.data.repository

import com.learnarm.core.database.dao.LetterDao
import com.learnarm.core.database.entity.LetterEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LetterRepository @Inject constructor(
    private val letterDao: LetterDao,
) {
    fun observeLetters(): Flow<List<LetterEntity>> = letterDao.observeAll()

    suspend fun getById(id: Int): LetterEntity? = letterDao.getById(id)
}
