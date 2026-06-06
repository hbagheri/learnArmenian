package com.learnarm.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_steps")
data class LessonStepEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val lessonId: Int,
    val orderIndex: Int,
    val type: String,
    val itemKey: String,
    val promptFa: String?,
)
