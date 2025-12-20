package com.example.news.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.news.data.api.NewsApiModule
import com.example.news.data.local.NewsDatabase
import com.example.news.data.mapper.toEntity
import com.example.news.data.mapper.toUiModel
import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of NewsRepository with Single Source of Truth (SSOT) pattern.
 * UI always reads from Room database via Paging 3.
 * Network is only used to refresh Room in background.
 */
class NewsRepositoryImpl(
    context: Context,
    private val newsApiService: com.example.news.data.api.NewsApiService = NewsApiModule.newsApiService
) : NewsRepository {
    
    private val database = NewsDatabase.getDatabase(context)
    private val articleDao = database.articleDao()
    
    override fun getPagedArticles(category: String): Flow<PagingData<ArticleUiModel>> {
        // Get PagingSource from Room and map entities to UI models
        // Room's PagingSource handles pagination automatically
        val pagingSourceFactory = { articleDao.getPagedArticlesByCategory(category) }
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow.map { pagingData -> 
            pagingData.map { entity -> entity.toUiModel() }
        }
    }
    
    override suspend fun refreshArticles(category: String) {
        try {
            // Fetch from network
            val response = newsApiService.getFeed(category = category)
            val articles = response.articles ?: emptyList()
            
            // Convert DTOs to entities and upsert to Room
            // Important: Each entity is tagged with the correct category from the API response
            val entities = articles.map { articleDto ->
                articleDto.toEntity(category = category)
            }
            
            // Upsert articles first
            articleDao.upsertArticles(entities)
            
            // Clean up old articles (keep only most recent 100 per category)
            // Use a reliable two-step approach: get IDs to keep, then delete the rest
            // This is more reliable than using LIMIT in a subquery
            val articleIdsToKeep = articleDao.getArticleIdsToKeep(category, keepLimit = 100)
            if (articleIdsToKeep.isNotEmpty()) {
                articleDao.deleteOldArticlesByExclusion(category, articleIdsToKeep)
            }
            
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
