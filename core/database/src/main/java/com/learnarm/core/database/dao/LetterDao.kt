package com.learnarm.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnarm.core.database.entity.LetterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDao {

    @Query("SELECT * FROM letters ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letters WHERE id = :id")
    suspend fun getById(id: Int): LetterEntity?

    @Query("SELECT COUNT(*) FROM letters")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(letters: List<LetterEntity>)
}
