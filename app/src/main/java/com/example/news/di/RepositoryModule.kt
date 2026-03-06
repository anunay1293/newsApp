package com.example.news.di

import com.example.news.data.repository.AuthRepositoryImpl
import com.example.news.data.repository.NewsRepositoryImpl
import com.example.news.domain.repository.AuthRepository
import com.example.news.domain.repository.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their concrete implementations.
 *
 * Uses [@Binds] for zero-overhead interface-to-implementation mapping, allowing
 * domain-layer use cases to depend on abstractions rather than concrete classes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
