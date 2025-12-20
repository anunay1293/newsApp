package com.example.news.data.local

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
     * Observe articles for a specific category, ordered by publishedAt descending (newest first).
     * This is the single source of truth for UI.
     */
    @Query("SELECT * FROM articles WHERE category = :category ORDER BY publishedAt DESC")
    fun observeArticlesByCategory(category: String): Flow<List<ArticleEntity>>
    
    /**
     * Upsert articles (insert or update if exists).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArticles(articles: List<ArticleEntity>)
    
    /**
     * Delete old articles for a category, keeping only the most recent N items.
     * Keeps the database size manageable by retaining only the latest articles per category.
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
    """)
    suspend fun deleteOldArticles(category: String, keepLimit: Int)
}

