package com.example.news.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the `articles` table.
 *
 * Provides CRUD, paging, and cleanup operations for locally cached news articles.
 * All read operations return either a Paging 3 [PagingSource] (for paginated UI lists)
 * or a reactive [Flow] (for real-time observation). Write operations are `suspend`
 * functions executed on a background dispatcher by Room.
 */
@Dao
interface ArticleDao {
    /**
     * Get paged articles for a specific category, ordered by publishedAt descending (newest first).
     * Optionally filters by search query (searches in title, author, and sourceName).
     * Used by Paging 3 for efficient pagination.
     */
    @Query("""
        SELECT * FROM articles 
        WHERE category = :category 
        AND (
            :searchQuery = '' OR
            title LIKE '%' || :searchQuery || '%' OR
            author LIKE '%' || :searchQuery || '%' OR
            sourceName LIKE '%' || :searchQuery || '%'
        )
        ORDER BY publishedAt DESC
    """)
    fun getPagedArticlesByCategory(category: String, searchQuery: String): PagingSource<Int, ArticleEntity>
    
    /**
     * Observe articles for a specific category, ordered by publishedAt descending (newest first).
     * This is the single source of truth for UI.
     * @deprecated Use getPagedArticlesByCategory with Paging 3 instead.
     */
    @Query("""
        SELECT * FROM articles 
        WHERE category = :category 
        ORDER BY publishedAt DESC
    """)
    fun observeArticlesByCategory(category: String): Flow<List<ArticleEntity>>
    
    /**
     * Upsert articles (insert or update if exists).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArticles(articles: List<ArticleEntity>)
    
    /**
     * Get article IDs to keep for a category (most recent N items).
     * Used internally by deleteOldArticles for reliable cleanup.
     */
    @Query("""
        SELECT articleId FROM articles 
        WHERE category = :category 
        ORDER BY publishedAt DESC 
        LIMIT :keepLimit
    """)
    suspend fun getArticleIdsToKeep(category: String, keepLimit: Int): List<String>
    
    /**
     * Delete old articles for a category, excluding the provided article IDs.
     * More reliable than subquery with LIMIT in SQLite.
     */
    @Query("""
        DELETE FROM articles 
        WHERE category = :category 
        AND articleId NOT IN (:articleIdsToKeep)
    """)
    suspend fun deleteOldArticlesByExclusion(category: String, articleIdsToKeep: List<String>)
    
    /**
     * Delete old articles for a category that are not bookmarked.
     * Keeps the most recent N articles per category, plus ALL bookmarked articles.
     * This ensures bookmarked articles are NEVER deleted, regardless of age or category.
     * Uses EXISTS for more reliable SQLite execution.
     */
    @Query("""
        DELETE FROM articles 
        WHERE category = :category 
        AND articleId NOT IN (
            SELECT articleId FROM articles 
            WHERE category = :category 
            ORDER BY publishedAt DESC 
            LIMIT :keepLimit
        )
        AND NOT EXISTS (
            SELECT 1 FROM bookmarks WHERE bookmarks.articleId = articles.articleId
        )
    """)
    suspend fun deleteOldNonBookmarkedArticles(category: String, keepLimit: Int)

    /**
     * Retrieves a single article by its unique [articleId].
     * Used by the article detail screen to load full article data from the local cache.
     *
     * @return The matching [ArticleEntity], or `null` if no article with that ID exists.
     */
    @Query("SELECT * FROM articles WHERE articleId = :articleId")
    suspend fun getArticleById(articleId: String): ArticleEntity?
}

