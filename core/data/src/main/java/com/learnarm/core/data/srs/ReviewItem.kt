package com.learnarm.core.data.srs

import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.core.database.entity.PhraseEntity

sealed interface ReviewItem {
    val key: String
    val frontPrimary: String
    val frontSecondary: String?
    val backTransliteration: String
    val backPersian: String

    data class Letter(val letter: LetterEntity) : ReviewItem {
        override val key: String get() = letterKey(letter.id)
        override val frontPrimary: String get() = "${letter.upper} ${letter.lower}"
        override val frontSecondary: String? get() = null
        override val backTransliteration: String get() = letter.nameLatin
        override val backPersian: String get() = letter.pronunciationFa
    }

    data class Phrase(val phrase: PhraseEntity) : ReviewItem {
        override val key: String get() = phraseKey(phrase.id)
        override val frontPrimary: String get() = phrase.armenian
        override val frontSecondary: String? get() = null
        override val backTransliteration: String get() = phrase.transliteration
        override val backPersian: String get() = phrase.persian
    }

    companion object {
        fun letterKey(id: Int): String = "letter:$id"
        fun phraseKey(id: Int): String = "phrase:$id"

        fun parseKind(key: String): Kind? {
            val idx = key.indexOf(':')
            if (idx <= 0) return null
            val id = key.substring(idx + 1).toIntOrNull() ?: return null
            return when (key.substring(0, idx)) {
                "letter" -> Kind.Letter(id)
                "phrase" -> Kind.Phrase(id)
                else -> null
            }
        }
    }

    sealed interface Kind {
        val id: Int
        data class Letter(override val id: Int) : Kind
        data class Phrase(override val id: Int) : Kind
    }
}
