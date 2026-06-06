package com.learnarm.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey val id: Int,
    val orderIndex: Int,
    val category: String,
    val armenian: String,
    val transliteration: String,
    val persian: String,
    val note: String? = null,
    val audioAsset: String? = null,
)
