package com.example.news.data.repository

import android.content.Context
import com.example.news.data.api.NewsApiModule
import com.example.news.data.local.NewsDatabase
import com.example.news.data.mapper.toEntity
import com.example.news.data.mapper.toUiModel
import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of NewsRepository with Single Source of Truth (SSOT) pattern.
 * UI always reads from Room database.
 * Network is only used to refresh Room in background.
 */
class NewsRepositoryImpl(
    context: Context,
    private val newsApiService: com.example.news.data.api.NewsApiService = NewsApiModule.newsApiService
) : NewsRepository {
    
    private val database = NewsDatabase.getDatabase(context)
    private val articleDao = database.articleDao()
    
    override fun observeArticles(category: String): Flow<List<ArticleUiModel>> {
        // Observe from Room - this is the single source of truth
        return articleDao.observeArticlesByCategory(category)
            .map { entities -> entities.map { it.toUiModel() } }
    }
    
    override suspend fun refreshArticles(category: String) {
        try {
            // Fetch from network
            val response = newsApiService.getFeed(category = category)
            val articles = response.articles ?: emptyList()
            
            // Convert DTOs to entities and upsert to Room
            val entities = articles.map { articleDto ->
                articleDto.toEntity(category = category)
            }
            
            articleDao.upsertArticles(entities)
            
            // Clean up old articles (keep only most recent 100 per category)
            articleDao.deleteOldArticles(category, keepLimit = 100)
            
        } catch (e: Exception) {
            // On error, don't throw exception - cached data remains available
            // UI continues showing cached articles from Room
            // Error is silently handled to maintain SSOT pattern
        }
    }
}

/**
 * Exception thrown when repository operations fail.
 */
class NewsRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
