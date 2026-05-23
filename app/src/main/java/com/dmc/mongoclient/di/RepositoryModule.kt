package com.dmc.mongoclient.di

import com.dmc.mongoclient.data.repo.BrowseRepositoryImpl
import com.dmc.mongoclient.data.repo.ConnectionRepositoryImpl
import com.dmc.mongoclient.data.repo.DocumentRepositoryImpl
import com.dmc.mongoclient.domain.repo.BrowseRepository
import com.dmc.mongoclient.domain.repo.ConnectionRepository
import com.dmc.mongoclient.domain.repo.DocumentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindBrowseRepository(impl: BrowseRepositoryImpl): BrowseRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository
}
