package com.learnarm.core.database.di

import android.content.Context
import androidx.room.Room
import com.learnarm.core.database.AppDatabase
import com.learnarm.core.database.dao.LessonDao
import com.learnarm.core.database.dao.LetterDao
import com.learnarm.core.database.dao.PhraseDao
import com.learnarm.core.database.dao.ReviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "learnarm.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideLetterDao(db: AppDatabase): LetterDao = db.letterDao()

    @Provides
    fun providePhraseDao(db: AppDatabase): PhraseDao = db.phraseDao()

    @Provides
    fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()

    @Provides
    fun provideLessonDao(db: AppDatabase): LessonDao = db.lessonDao()
}
