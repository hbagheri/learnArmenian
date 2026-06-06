package com.learnarm.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Query("SELECT * FROM lessons ORDER BY orderIndex ASC")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLesson(id: Int): LessonEntity?

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons")
    suspend fun clearLessons()

    @Query("SELECT * FROM lesson_steps WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    suspend fun getSteps(lessonId: Int): List<LessonStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<LessonStepEntity>)

    @Query("DELETE FROM lesson_steps")
    suspend fun clearSteps()
}
