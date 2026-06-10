package com.learnarm.core.data.repository

import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.api.LetterDto
import com.learnarm.core.database.dao.LetterDao
import com.learnarm.core.database.entity.LetterEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LetterRepository @Inject constructor(
    private val letterDao: LetterDao,
    private val curriculumService: CurriculumService,
) {
    fun observeLetters(): Flow<List<LetterEntity>> = letterDao.observeAll()

    suspend fun getById(id: Int): LetterEntity? = letterDao.getById(id)

    /**
     * Fetches the latest letter data from the backend and upserts into Room.
     * Silently no-ops on network error so the existing seeded copy stays usable offline.
     */
    suspend fun syncFromBackend() {
        runCatching { curriculumService.getLetters() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { dtos -> letterDao.insertAll(dtos.map { it.toEntity() }) }
    }

    private fun LetterDto.toEntity() = LetterEntity(
        id = id,
        orderIndex = orderIndex,
        upper = upper,
        lower = lower,
        name = name,
        nameLatin = nameLatin,
        pronunciationFa = pronunciationFa,
        ipa = ipa,
        exampleArmenian = exampleArmenian,
        exampleLatin = exampleLatin,
        exampleFa = exampleFa,
        audioAsset = audioAsset,
    )
}
