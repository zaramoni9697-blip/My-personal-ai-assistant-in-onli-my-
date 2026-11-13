package com.jarvis.ai.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

// --- Entities ---
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "providers",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")]
)
data class ProviderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val providerType: String, // gemini, openai, claude, etc.
    val displayName: String,
    val apiKeyEncrypted: String,
    val modelId: String,
    val baseUrl: String?,
    val isActive: Boolean = false,
    val lastTestStatus: String? = null,
    val lastTestAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")]
)
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val providerId: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages",
    foreignKeys = [ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // user, assistant, system
    val content: String,
    val providerType: String? = null,
    val modelId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// --- DAOs ---
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(u: UserEntity)
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1") suspend fun getByEmail(email: String): UserEntity?
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1") suspend fun getById(id: String): UserEntity?
    @Query("DELETE FROM users WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface ProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(p: ProviderEntity)
    @Update suspend fun update(p: ProviderEntity)
    @Query("DELETE FROM providers WHERE id = :id") suspend fun deleteById(id: String)
    @Query("SELECT * FROM providers WHERE userId = :userId ORDER BY createdAt DESC") fun observeForUser(userId: String): Flow<List<ProviderEntity>>
    @Query("SELECT * FROM providers WHERE userId = :userId") suspend fun getForUser(userId: String): List<ProviderEntity>
    @Query("SELECT * FROM providers WHERE userId = :userId AND isActive = 1 LIMIT 1") suspend fun getActive(userId: String): ProviderEntity?
    @Query("UPDATE providers SET isActive = 0 WHERE userId = :userId") suspend fun deactivateAll(userId: String)
    @Query("UPDATE providers SET isActive = 1 WHERE id = :id") suspend fun activate(id: String)
}

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(c: ConversationEntity)
    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC") fun observeForUser(userId: String): Flow<List<ConversationEntity>>
    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1") suspend fun getById(id: String): ConversationEntity?
    @Query("DELETE FROM conversations WHERE id = :id") suspend fun deleteById(id: String)
    @Query("UPDATE conversations SET updatedAt = :ts WHERE id = :id") suspend fun touch(id: String, ts: Long = System.currentTimeMillis())
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: MessageEntity)
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC") fun observeForConversation(cid: String): Flow<List<MessageEntity>>
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC") suspend fun getForConversation(cid: String): List<MessageEntity>
    @Query("DELETE FROM messages WHERE conversationId = :cid") suspend fun deleteForConversation(cid: String)
}

// --- Database ---
@Database(
    entities = [UserEntity::class, ProviderEntity::class, ConversationEntity::class, MessageEntity::class],
    version = 1, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun providerDao(): ProviderDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}

class Converters
