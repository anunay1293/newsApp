package com.example.news.data.repository

import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for news data.
 * Implements Single Source of Truth (SSOT) pattern:
 * - UI always reads from Room database (via Flow)
 * - Network is only used to refresh Room in background
 */
interface NewsRepository {
    /**
     * Observe articles for a category from Room database.
     * This is the single source of truth for UI.
     */
    fun observeArticles(category: String): Flow<List<ArticleUiModel>>
    
    /**
     * Refresh articles for a category from network and update Room.
     * This runs in background and updates Room, which automatically notifies Flow observers.
     * On error, cached data remains available (no exception thrown).
     */
    suspend fun refreshArticles(category: String)
}
