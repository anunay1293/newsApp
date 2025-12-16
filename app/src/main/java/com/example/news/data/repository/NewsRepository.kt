package com.example.news.data.repository

import com.example.news.ui.model.ArticleUiModel

/**
 * Repository interface for news data.
 */
interface NewsRepository {
    suspend fun fetchTopHeadlinesByCategory(category: String): List<ArticleUiModel>
}

