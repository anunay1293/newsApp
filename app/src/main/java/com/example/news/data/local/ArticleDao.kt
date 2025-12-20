package com.example.news.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for article database operations.
 */
@Dao
interface ArticleDao {
    /**
     * Get paged articles for a specific category, ordered by publishedAt descending (newest first).
     * Used by Paging 3 for efficient pagination.
     */
    @Query("SELECT * FROM articles WHERE category = :category ORDER BY publishedAt DESC")
    fun getPagedArticlesByCategory(category: String): PagingSource<Int, ArticleEntity>
    
    /**
     * Observe articles for a specific category, ordered by publishedAt descending (newest first).
     * This is the single source of truth for UI.
     * @deprecated Use getPagedArticlesByCategory with Paging 3 instead.
     */
    @Query("SELECT * FROM articles WHERE category = :category ORDER BY publishedAt DESC")
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
}

