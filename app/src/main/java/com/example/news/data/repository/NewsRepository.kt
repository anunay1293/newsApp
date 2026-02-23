package com.example.news.data.repository

import androidx.paging.PagingData
import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for all news-related data operations.
 *
 * Follows the **Single Source of Truth (SSOT)** pattern:
 * - **Reads** always come from the local Room database, exposed as reactive [PagingData] flows.
 * - **Writes** fetch fresh data from the network and upsert it into Room; Room then
 *   automatically notifies all active PagingData observers.
 *
 * This design ensures that the UI always has data to show (cached articles) even when the
 * network is unavailable, while seamlessly updating when connectivity is restored.
 *
 * @see NewsRepositoryImpl for the concrete implementation.
 */
interface NewsRepository {

    /**
     * Returns a reactive, paginated stream of articles for the given [category].
     *
     * The data is sourced from the Room database, making this the single source of truth
     * for the UI. An optional [searchQuery] filters results by title, author, or source name.
     *
     * @param category    The news category to query (e.g., "general", "technology").
     * @param searchQuery Free-text filter applied to title, author, and source name columns.
     *                    Pass an empty string to disable filtering.
     * @return A [Flow] emitting [PagingData] of [ArticleUiModel] that updates whenever
     *         the underlying Room table changes.
     */
    fun getPagedArticles(category: String, searchQuery: String = ""): Flow<PagingData<ArticleUiModel>>
    
    /**
     * Fetches the latest articles for [category] from the remote API and upserts them
     * into the Room database.
     *
     * This is a **fire-and-forget** background operation: on success Room notifies
     * PagingData observers; on failure the error is silently swallowed so cached data
     * remains available to the user.
     *
     * @param category The news category to refresh (e.g., "sports").
     */
    suspend fun refreshArticles(category: String)
    
    /**
     * Toggles the bookmark state for the article identified by [articleId].
     *
     * If the article is currently bookmarked it will be un-bookmarked, and vice versa.
     * The bookmark change invalidates the active PagingSource so that both the feed and
     * bookmarks screens reflect the new state automatically.
     *
     * @param articleId The unique identifier of the article to toggle.
     */
    suspend fun toggleBookmark(articleId: String)
    
    /**
     * Returns a reactive, paginated stream of all bookmarked articles, ordered by the
     * time they were bookmarked (most recent first).
     *
     * The flow re-emits whenever the bookmarks table changes (e.g., a bookmark is added
     * or removed), triggering a fresh PagingSource for the UI.
     *
     * @return A [Flow] of [PagingData] containing only bookmarked [ArticleUiModel] items.
     */
    fun getPagedBookmarkedArticles(): Flow<PagingData<ArticleUiModel>>
}
