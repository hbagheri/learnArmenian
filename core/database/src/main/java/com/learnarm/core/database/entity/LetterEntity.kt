package com.learnarm.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "letters")
data class LetterEntity(
    @PrimaryKey val id: Int,
    val orderIndex: Int,
    val upper: String,
    val lower: String,
    val name: String,
    val nameLatin: String,
    val pronunciationFa: String,
    val ipa: String,
    val exampleArmenian: String,
    val exampleLatin: String,
    val exampleFa: String,
    val audioAsset: String? = null,
)
