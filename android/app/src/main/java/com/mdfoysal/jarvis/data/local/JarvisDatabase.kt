package com.mdfoysal.jarvis.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mdfoysal.jarvis.data.local.dao.ConversationDao
import com.mdfoysal.jarvis.data.local.dao.MemoryDao
import com.mdfoysal.jarvis.data.local.dao.MessageDao
import com.mdfoysal.jarvis.data.local.dao.ProviderConfigDao
import com.mdfoysal.jarvis.data.local.dao.UserDao
import com.mdfoysal.jarvis.data.local.entity.ConversationEntity
import com.mdfoysal.jarvis.data.local.entity.MemoryEntity
import com.mdfoysal.jarvis.data.local.entity.MessageEntity
import com.mdfoysal.jarvis.data.local.entity.ProviderConfigEntity
import com.mdfoysal.jarvis.data.local.entity.UserEntity

/** The local JARVIS database (accounts, provider configs, conversations, memories). */
@Database(
    entities = [
        UserEntity::class,
        ProviderConfigEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun providerConfigDao(): ProviderConfigDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        private const val DB_NAME = "jarvis.db"

        fun build(context: Context): JarvisDatabase =
            Room.databaseBuilder(context.applicationContext, JarvisDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // Foreign keys stay off by default in SQLite; conversations are cleaned
                        // up explicitly by the repositories, so we only enable WAL for speed.
                        db.execSQL("PRAGMA journal_mode=WAL")
                    }
                })
                .build()
    }
}
