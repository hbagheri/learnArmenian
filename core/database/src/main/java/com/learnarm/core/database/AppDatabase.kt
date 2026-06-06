package com.learnarm.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.learnarm.core.database.dao.LessonDao
import com.learnarm.core.database.dao.LetterDao
import com.learnarm.core.database.dao.PhraseDao
import com.learnarm.core.database.dao.ReviewDao
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.core.database.entity.PhraseEntity
import com.learnarm.core.database.entity.ReviewEntity

@Database(
    entities = [
        LetterEntity::class,
        PhraseEntity::class,
        ReviewEntity::class,
        LessonEntity::class,
        LessonStepEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun letterDao(): LetterDao
    abstract fun phraseDao(): PhraseDao
    abstract fun reviewDao(): ReviewDao
    abstract fun lessonDao(): LessonDao
}
