package com.example.news.data.repository

import androidx.paging.PagingData
import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for news data.
 * Implements Single Source of Truth (SSOT) pattern:
 * - UI always reads from Room database (via PagingData)
 * - Network is only used to refresh Room in background
 */
interface NewsRepository {
    /**
     * Get paged articles for a category from Room database.
     * Optionally filters by search query (searches in title, author, and sourceName).
     * This is the single source of truth for UI.
     * Returns PagingData for efficient pagination.
     */
    fun getPagedArticles(category: String, searchQuery: String = ""): Flow<PagingData<ArticleUiModel>>
    
    /**
     * Refresh articles for a category from network and update Room.
     * This runs in background and updates Room, which automatically notifies PagingData observers.
     * On error, cached data remains available (no exception thrown).
     */
    suspend fun refreshArticles(category: String)
    
    /**
     * Toggle bookmark state for an article.
     * If bookmarked, removes bookmark. If not bookmarked, adds bookmark.
     */
    suspend fun toggleBookmark(articleId: String)
    
    /**
     * Get all bookmarked articles as PagingData.
     */
    fun getPagedBookmarkedArticles(): Flow<PagingData<ArticleUiModel>>
}
