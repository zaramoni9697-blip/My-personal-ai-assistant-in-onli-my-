package com.mdfoysal.jarvis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mdfoysal.jarvis.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeForUser(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun listForUser(userId: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestForUser(userId: String): ConversationEntity?

    @Query("SELECT COUNT(*) FROM conversations WHERE userId = :userId")
    suspend fun countForUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET updatedAt = :timestamp, title = COALESCE(NULLIF(:title, ''), title) WHERE id = :id")
    suspend fun touch(id: String, timestamp: Long, title: String?)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversations WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
