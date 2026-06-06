package com.learnarm.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: Int,
    val orderIndex: Int,
    val moduleKey: String,
    val titleFa: String,
    val subtitleFa: String?,
    val estMinutes: Int,
    val prerequisiteIdsCsv: String,
)
