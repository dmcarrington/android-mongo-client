package com.dmc.mongoclient.di

import android.content.Context
import com.dmc.mongoclient.data.settings.AppSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideAppSettings(@ApplicationContext context: Context): AppSettings = AppSettings(context)
}
