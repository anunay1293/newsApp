package com.example.news.data.repository

import com.example.news.data.api.NewsApiModule
import com.example.news.data.mapper.toUiModel
import com.example.news.ui.model.ArticleUiModel

/**
 * Implementation of NewsRepository.
 * Fetches articles from AWS API Gateway and maps them to UI models.
 */
class NewsRepositoryImpl(
    private val newsApiService: com.example.news.data.api.NewsApiService = NewsApiModule.newsApiService
) : NewsRepository {
    
    override suspend fun fetchTopHeadlinesByCategory(category: String): List<ArticleUiModel> {
        return try {
            val response = newsApiService.getFeed(category = category)
            response.articles?.mapNotNull { articleDto ->
                articleDto.toUiModel()
            } ?: emptyList()
        } catch (e: Exception) {
            throw NewsRepositoryException("Failed to fetch articles: ${e.message}", e)
        }
    }
}

/**
 * Exception thrown when repository operations fail.
 */
class NewsRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

