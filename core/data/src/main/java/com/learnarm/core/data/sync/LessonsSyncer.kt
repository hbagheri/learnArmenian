package com.learnarm.core.data.sync

import com.learnarm.core.database.dao.LessonDao
import com.learnarm.core.database.dao.PhraseDao
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import com.learnarm.core.database.entity.PhraseEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonsSyncer @Inject constructor(
    private val apiClient: LessonsApiClient,
    private val lessonDao: LessonDao,
    private val phraseDao: PhraseDao,
    private val versionStore: ContentVersionStore,
) {
    /**
     * Fetch /content/lessons and replace local lesson + step rows when the
     * backend reports a newer version. Extra phrases (e.g. taxi-specific) are
     * upserted into the phrases table alongside the bootstrap seed. Silent on
     * network errors — UI falls back to whatever Room already has.
     */
    suspend fun syncIfNeeded(): SyncOutcome {
        val localCount = lessonDao.count()
        val remoteVersion = apiClient.fetchVersion() ?: return SyncOutcome.NetworkSkipped
        val localVersion = versionStore.getCurrentVersion()
        if (localCount > 0 && localVersion >= remoteVersion) {
            return SyncOutcome.AlreadyUpToDate(localVersion)
        }
        val payload = apiClient.fetchLessons() ?: return SyncOutcome.NetworkSkipped
        applyPayload(payload)
        versionStore.setCurrentVersion(payload.version)
        return SyncOutcome.Synced(
            version = payload.version,
            lessonCount = payload.lessons.size,
            stepCount = payload.lessons.sumOf { it.steps.size },
            extraPhraseCount = payload.extraPhrases.size,
        )
    }

    private suspend fun applyPayload(payload: LessonsResponseDto) {
        if (payload.extraPhrases.isNotEmpty()) {
            phraseDao.insertAll(payload.extraPhrases.map { it.toEntity() })
        }
        lessonDao.clearSteps()
        lessonDao.clearLessons()
        if (payload.lessons.isNotEmpty()) {
            lessonDao.insertLessons(payload.lessons.map { it.toEntity() })
            val allSteps = payload.lessons.flatMap { lesson ->
                lesson.steps.map { step -> step.toEntity(lessonId = lesson.id) }
            }
            if (allSteps.isNotEmpty()) lessonDao.insertSteps(allSteps)
        }
    }
}

sealed interface SyncOutcome {
    data object NetworkSkipped : SyncOutcome
    data class AlreadyUpToDate(val localVersion: Int) : SyncOutcome
    data class Synced(
        val version: Int,
        val lessonCount: Int,
        val stepCount: Int,
        val extraPhraseCount: Int,
    ) : SyncOutcome
}

private fun ExtraPhraseDto.toEntity(): PhraseEntity = PhraseEntity(
    id = id,
    orderIndex = orderIndex,
    category = category,
    armenian = armenian,
    transliteration = transliteration,
    persian = persian,
    note = note,
    audioAsset = audioAsset,
)

private fun LessonDto.toEntity(): LessonEntity = LessonEntity(
    id = id,
    orderIndex = orderIndex,
    moduleKey = moduleKey,
    titleFa = titleFa,
    subtitleFa = subtitleFa,
    estMinutes = estMinutes,
    prerequisiteIdsCsv = prerequisiteIds.joinToString(","),
)

private fun LessonStepDto.toEntity(lessonId: Int): LessonStepEntity = LessonStepEntity(
    lessonId = lessonId,
    orderIndex = orderIndex,
    type = type,
    itemKey = itemKey,
    promptFa = promptFa,
)
