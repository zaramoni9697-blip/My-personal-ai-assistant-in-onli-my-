package com.mdfoysal.jarvis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mdfoysal.jarvis.data.local.entity.ProviderConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderConfigDao {

    @Query("SELECT * FROM provider_configs WHERE userId = :userId ORDER BY createdAt ASC")
    fun observeForUser(userId: String): Flow<List<ProviderConfigEntity>>

    @Query("SELECT * FROM provider_configs WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun listForUser(userId: String): List<ProviderConfigEntity>

    @Query("SELECT * FROM provider_configs WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProviderConfigEntity?

    @Query("SELECT * FROM provider_configs WHERE userId = :userId AND isActive = 1 LIMIT 1")
    fun observeActive(userId: String): Flow<ProviderConfigEntity?>

    @Query("SELECT * FROM provider_configs WHERE userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun activeForUser(userId: String): ProviderConfigEntity?

    @Query("SELECT COUNT(*) FROM provider_configs WHERE userId = :userId")
    suspend fun countForUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ProviderConfigEntity)

    @Update
    suspend fun update(config: ProviderConfigEntity)

    @Query("UPDATE provider_configs SET isActive = 0 WHERE userId = :userId")
    suspend fun clearActive(userId: String)

    @Query("UPDATE provider_configs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("DELETE FROM provider_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM provider_configs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
