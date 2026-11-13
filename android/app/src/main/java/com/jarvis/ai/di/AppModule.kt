package com.jarvis.ai.di

import android.content.Context
import androidx.room.Room
import com.jarvis.ai.data.local.db.JarvisDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): JarvisDatabase =
        Room.databaseBuilder(ctx, JarvisDatabase::class.java, "jarvis.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: JarvisDatabase) = db.userDao()
    @Provides fun provideProviderDao(db: JarvisDatabase) = db.providerDao()
    @Provides fun provideConversationDao(db: JarvisDatabase) = db.conversationDao()
    @Provides fun provideMessageDao(db: JarvisDatabase) = db.messageDao()
}
