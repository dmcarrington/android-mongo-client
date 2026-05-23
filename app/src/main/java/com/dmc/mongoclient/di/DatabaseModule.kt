package com.dmc.mongoclient.di

import android.content.Context
import androidx.room.Room
import com.dmc.mongoclient.data.db.AppDatabase
import com.dmc.mongoclient.data.db.ConnectionDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "mongo-client.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideConnectionDao(db: AppDatabase): ConnectionDao = db.connectionDao()
}
