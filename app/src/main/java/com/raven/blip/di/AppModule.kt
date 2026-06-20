package com.raven.blip.di

import android.content.Context
import androidx.room.Room
import com.raven.blip.data.local.BlipDatabase
import com.raven.blip.data.local.TaskDao
import com.raven.blip.data.repository.TaskRepositoryImpl
import com.raven.blip.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    // Bind the interface to its implementation
    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): BlipDatabase =
            Room.databaseBuilder(
                context,
                BlipDatabase::class.java,
                "blip.db"
            ).build()

        @Provides
        fun provideTaskDao(db: BlipDatabase): TaskDao = db.taskDao()
    }
}
