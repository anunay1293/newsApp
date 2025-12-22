package com.example.news.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for bookmark database operations.
 */
@Dao
interface BookmarkDao {
    /**
     * Insert a bookmark for an article.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)
    
    /**
     * Delete a bookmark for an article.
     */
    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
    
    /**
     * Check if an article is bookmarked.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE articleId = :articleId)")
    fun isBookmarked(articleId: String): Flow<Boolean>
    
    /**
     * Get all bookmarked articles as a PagingSource.
     * Joins with articles table to get full article data.
     */
    @Query("""
        SELECT a.* FROM articles a
        INNER JOIN bookmarks b ON a.articleId = b.articleId
        ORDER BY b.bookmarkedAt DESC
    """)
    fun getPagedBookmarkedArticles(): PagingSource<Int, ArticleEntity>
    
    /**
     * Get all bookmarked article IDs.
     */
    @Query("SELECT articleId FROM bookmarks")
    suspend fun getAllBookmarkedIds(): List<String>
    
    /**
     * Observe all bookmarked article IDs as a Flow.
     * This allows reactive updates when bookmarks change.
     */
    @Query("SELECT articleId FROM bookmarks")
    fun observeAllBookmarkedIds(): Flow<List<String>>
    
    /**
     * Get bookmark entity by articleId.
     */
    @Query("SELECT * FROM bookmarks WHERE articleId = :articleId")
    suspend fun getBookmark(articleId: String): BookmarkEntity?
}

