package com.learnarm.core.data.seed

import com.learnarm.core.database.entity.PhraseEntity
import kotlinx.serialization.Serializable

@Serializable
internal data class PhraseSeed(
    val id: Int,
    val orderIndex: Int,
    val category: String,
    val armenian: String,
    val transliteration: String,
    val persian: String,
    val note: String? = null,
    val audioAsset: String? = null,
) {
    fun toEntity(): PhraseEntity = PhraseEntity(
        id = id,
        orderIndex = orderIndex,
        category = category,
        armenian = armenian,
        transliteration = transliteration,
        persian = persian,
        note = note,
        audioAsset = audioAsset,
    )
}
