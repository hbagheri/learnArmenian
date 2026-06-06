package com.learnarm.core.data.repository

import com.learnarm.core.database.dao.LessonDao
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonRepository @Inject constructor(
    private val lessonDao: LessonDao,
) {
    fun observeLessons(): Flow<List<LessonEntity>> = lessonDao.observeLessons()

    suspend fun getLesson(id: Int): LessonEntity? = lessonDao.getLesson(id)

    suspend fun getSteps(lessonId: Int): List<LessonStepEntity> = lessonDao.getSteps(lessonId)

    suspend fun count(): Int = lessonDao.count()
}
