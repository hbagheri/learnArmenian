package com.learnarm.core.data.repository

import com.learnarm.core.data.srs.ReviewItem
import com.learnarm.core.data.srs.ReviewQuality
import com.learnarm.core.data.srs.Sm2
import com.learnarm.core.data.srs.Sm2State
import com.learnarm.core.database.dao.ReviewDao
import com.learnarm.core.database.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val reviewDao: ReviewDao,
    private val letterRepository: LetterRepository,
    private val phraseRepository: PhraseRepository,
) {
    fun observeDueCount(now: Long = System.currentTimeMillis()): Flow<Int> =
        reviewDao.observeDueCount(now)

    suspend fun nextDueItem(now: Long = System.currentTimeMillis()): ReviewItem? {
        val entity = reviewDao.nextDueBefore(now) ?: return null
        return loadItem(entity.itemKey)
    }

    suspend fun recordReview(
        key: String,
        quality: ReviewQuality,
        now: Long = System.currentTimeMillis(),
    ) {
        val existing = reviewDao.getByKey(key)
        val previousState = existing?.let {
            Sm2State(
                ease = it.ease,
                intervalDays = it.intervalDays,
                repetitions = it.repetitions,
            )
        } ?: Sm2.initialState()
        val nextState = Sm2.step(previousState, quality)
        val nextDue = now + nextState.intervalDays * MS_PER_DAY
        reviewDao.upsert(
            ReviewEntity(
                itemKey = key,
                ease = nextState.ease,
                intervalDays = nextState.intervalDays,
                repetitions = nextState.repetitions,
                dueAt = nextDue,
                lastReviewedAt = now,
            ),
        )
    }

    suspend fun loadItem(key: String): ReviewItem? {
        val kind = ReviewItem.parseKind(key) ?: return null
        return when (kind) {
            is ReviewItem.Kind.Letter -> letterRepository.getById(kind.id)?.let(ReviewItem::Letter)
            is ReviewItem.Kind.Phrase -> phraseRepository.getById(kind.id)?.let(ReviewItem::Phrase)
        }
    }

    private companion object {
        const val MS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
