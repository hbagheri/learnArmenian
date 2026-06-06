package com.learnarm.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val itemKey: String,
    val ease: Float,
    val intervalDays: Int,
    val repetitions: Int,
    val dueAt: Long,
    val lastReviewedAt: Long?,
)
