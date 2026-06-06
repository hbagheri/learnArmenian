package com.learnarm.core.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class LessonsResponseDto(
    val version: Int,
    val extraPhrases: List<ExtraPhraseDto> = emptyList(),
    val lessons: List<LessonDto> = emptyList(),
)

@Serializable
data class ExtraPhraseDto(
    val id: Int,
    val orderIndex: Int,
    val category: String,
    val armenian: String,
    val transliteration: String,
    val persian: String,
    val note: String? = null,
    val audioAsset: String? = null,
)

@Serializable
data class LessonDto(
    val id: Int,
    val orderIndex: Int,
    val moduleKey: String,
    val titleFa: String,
    val subtitleFa: String? = null,
    val estMinutes: Int,
    val prerequisiteIds: List<Int> = emptyList(),
    val steps: List<LessonStepDto> = emptyList(),
)

@Serializable
data class LessonStepDto(
    val orderIndex: Int,
    val type: String,
    val itemKey: String,
    val promptFa: String? = null,
)
