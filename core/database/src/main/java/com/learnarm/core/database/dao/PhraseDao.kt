package com.learnarm.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnarm.core.database.entity.PhraseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {

    @Query("SELECT * FROM phrases ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases WHERE category = :category ORDER BY orderIndex ASC")
    fun observeByCategory(category: String): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases WHERE id = :id")
    suspend fun getById(id: Int): PhraseEntity?

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phrases: List<PhraseEntity>)
}
