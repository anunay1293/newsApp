package com.example.news.domain.repository

import androidx.paging.PagingData
import com.example.news.domain.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for all news-related data operations.
 *
 * Follows the **Single Source of Truth (SSOT)** pattern:
 * - **Reads** always come from the local Room database, exposed as reactive [PagingData] flows.
 * - **Writes** fetch fresh data from the network and upsert it into Room; Room then
 *   automatically notifies all active PagingData observers.
 *
 * This interface lives in the domain layer so that use cases depend on an abstraction
 * rather than a concrete data-layer implementation.
 */
interface NewsRepository {

    /**
     * Returns a reactive, paginated stream of [Article] objects for the given [category].
     *
     * Data is sourced from the Room database (single source of truth). An optional
     * [searchQuery] filters results by title, author, or source name.
     *
     * @param category    The news category to query (e.g., "general", "technology").
     * @param searchQuery Free-text filter applied to title, author, and source name columns.
     *                    Pass an empty string to disable filtering.
     * @return A [Flow] emitting [PagingData] of [Article] that updates whenever
     *         the underlying Room table changes.
     */
    fun getPagedArticles(category: String, searchQuery: String = ""): Flow<PagingData<Article>>

    /**
     * Fetches the latest articles for [category] from the remote API and upserts them
     * into the Room database.
     *
     * On success Room notifies PagingData observers automatically. On failure the error
     * is silently swallowed so cached data remains available to the user.
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
     * The flow re-emits whenever the bookmarks table changes, triggering a fresh
     * PagingSource for the consumer.
     *
     * @return A [Flow] of [PagingData] containing only bookmarked [Article] items.
     */
    fun getPagedBookmarkedArticles(): Flow<PagingData<Article>>

    /**
     * Retrieves a single [Article] by its unique [articleId] from the local Room database.
     *
     * Used by the article detail screen to load full article data (title, URL, bookmark
     * state, etc.) for in-app display.
     *
     * @param articleId The unique identifier of the article to retrieve.
     * @return The matching [Article], or `null` if no article with that ID exists locally.
     */
    suspend fun getArticleById(articleId: String): Article?

    /**
     * Pulls all bookmarks from DynamoDB and replaces the local bookmark set.
     *
     * For each remote bookmark, upserts the associated article into Room (to satisfy the
     * foreign key), then replaces local bookmarks with the remote set.
     */
    suspend fun syncBookmarksFromRemote()

    /**
     * Pushes a single bookmark change to DynamoDB (fire-and-forget).
     *
     * @param articleId    The article whose bookmark state changed.
     * @param isBookmarked `true` if the article was just bookmarked, `false` if un-bookmarked.
     */
    suspend fun syncBookmarkToRemote(articleId: String, isBookmarked: Boolean)

    /**
     * Removes all local bookmark data and resets the in-memory bookmark-ID cache.
     *
     * Called on sign-out to prevent a previous user's bookmarks from appearing on the
     * feed while no user is authenticated. Remote bookmarks in DynamoDB are not affected;
     * they will be re-synced via [syncBookmarksFromRemote] on the next sign-in.
     */
    suspend fun clearLocalBookmarks()
}
