package com.mdfoysal.jarvis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mdfoysal.jarvis.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC")
    fun observeForUser(userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC LIMIT :limit")
    suspend fun limitForUser(userId: String, limit: Int): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM memories WHERE userId = :userId")
    suspend fun countForUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    @Query("UPDATE memories SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM memories WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
