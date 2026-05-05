package com.example.news.di

import com.example.news.data.repository.ArticleSummaryRepositoryImpl
import com.example.news.data.repository.AuthRepositoryImpl
import com.example.news.data.repository.NewsRepositoryImpl
import com.example.news.domain.repository.ArticleSummaryRepository
import com.example.news.domain.repository.AuthRepository
import com.example.news.domain.repository.NewsRepository
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
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindArticleSummaryRepository(impl: ArticleSummaryRepositoryImpl): ArticleSummaryRepository
}
