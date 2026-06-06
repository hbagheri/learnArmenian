package com.learnarm.core.data.seed

import com.learnarm.core.database.entity.LetterEntity
import kotlinx.serialization.Serializable

@Serializable
internal data class LetterSeed(
    val id: Int,
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
) {
    fun toEntity(): LetterEntity = LetterEntity(
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
