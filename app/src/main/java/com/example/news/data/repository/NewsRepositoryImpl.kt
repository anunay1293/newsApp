package com.example.news.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.news.data.api.NewsApiModule
import com.example.news.data.local.BookmarkEntity
import com.example.news.data.local.NewsDatabase
import com.example.news.data.mapper.toEntity
import com.example.news.data.mapper.toUiModel
import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Implementation of NewsRepository with Single Source of Truth (SSOT) pattern.
 * UI always reads from Room database via Paging 3.
 * Network is only used to refresh Room in background.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NewsRepositoryImpl(
    context: Context,
    private val newsApiService: com.example.news.data.api.NewsApiService = NewsApiModule.newsApiService
) : NewsRepository {
    
    private val database = NewsDatabase.getDatabase(context)
    private val articleDao = database.articleDao()
    private val bookmarkDao = database.bookmarkDao()
    
    // Cache of bookmarked article IDs for efficient lookup (used for feed screen bookmark icons)
    private val bookmarkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    
    init {
        // Load initial bookmarked IDs asynchronously for feed screen
        bookmarkScope.launch {
            refreshBookmarkedIds()
        }
    }
    
    private suspend fun refreshBookmarkedIds() {
        // Query all bookmarked article IDs and update cache (for feed screen)
        val bookmarkedIdsList = bookmarkDao.getAllBookmarkedIds()
        _bookmarkedIds.value = bookmarkedIdsList.toSet()
    }
    
    // Observe bookmarks from Room directly - this ensures all repository instances see the same data
    // This is used for the bookmarks screen to react to bookmark changes in real-time
    private val bookmarkedIdsFromRoom: Flow<Set<String>> = bookmarkDao.observeAllBookmarkedIds()
        .map { it.toSet() }
        .distinctUntilChanged()
    
    override fun getPagedArticles(category: String, searchQuery: String): Flow<PagingData<ArticleUiModel>> {
        // Refresh bookmarked IDs when starting to observe articles
        bookmarkScope.launch {
            refreshBookmarkedIds()
        }
        
        // Use flatMapLatest on bookmarkedIds to create a new Pager flow when bookmarks change
        // This ensures we don't try to collect from the same Pager.flow instance twice
        return _bookmarkedIds.flatMapLatest { bookmarkedSet ->
            // Create a new Pager each time bookmarks change to avoid "collect twice" error
            val pagingSourceFactory = { articleDao.getPagedArticlesByCategory(category, searchQuery) }
            
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                    prefetchDistance = 10
                ),
                pagingSourceFactory = pagingSourceFactory
            ).flow.map { pagingData ->
                pagingData.map { entity -> 
                    entity.toUiModel(isBookmarked = bookmarkedSet.contains(entity.articleId))
                }
            }
        }
    }
    
    override suspend fun refreshArticles(category: String) {
        try {
            // Fetch from network
            val response = newsApiService.getFeed(category = category)
            val articles = response.articles ?: emptyList()
            
            // Convert DTOs to entities and upsert to Room
            // Important: Each entity is tagged with the correct category from the API response
            val entities = articles.map { articleDto ->
                articleDto.toEntity(category = category)
            }
            
            // Upsert articles first
            articleDao.upsertArticles(entities)
            
            // Clean up old articles (keep only most recent 100 per category)
            // IMPORTANT: NEVER delete bookmarked articles, regardless of category or age
            // Use a SQL query that explicitly excludes bookmarked articles from deletion
            articleDao.deleteOldNonBookmarkedArticles(category, keepLimit = 100)
            
        } catch (e: Exception) {
            // On error, don't throw exception - cached data remains available
            // UI continues showing cached articles from Room
            // Error is silently handled to maintain SSOT pattern
        }
    }
    
    override suspend fun toggleBookmark(articleId: String) {
        val existingBookmark = bookmarkDao.getBookmark(articleId)
        if (existingBookmark != null) {
            // Remove bookmark
            bookmarkDao.deleteBookmark(existingBookmark)
            // Update cache immediately
            _bookmarkedIds.value = _bookmarkedIds.value - articleId
        } else {
            // Add bookmark
            bookmarkDao.insertBookmark(BookmarkEntity(articleId = articleId))
            // Update cache immediately
            _bookmarkedIds.value = _bookmarkedIds.value + articleId
        }
        // Also refresh from DB to ensure consistency
        refreshBookmarkedIds()
    }
    
    override fun getPagedBookmarkedArticles(): Flow<PagingData<ArticleUiModel>> {
        // Observe bookmarks directly from Room - this ensures we always see the latest state
        // regardless of which repository instance we're using
        // When bookmarks change in Room, this Flow will emit, triggering a new Pager
        return bookmarkedIdsFromRoom
            .flatMapLatest {
                // Create a new Pager each time bookmarks change to ensure fresh data
                val pagingSourceFactory = { bookmarkDao.getPagedBookmarkedArticles() }
                
                Pager(
                    config = PagingConfig(
                        pageSize = 20,
                        enablePlaceholders = false,
                        prefetchDistance = 10
                    ),
                    pagingSourceFactory = pagingSourceFactory
                ).flow.map { pagingData -> 
                    // All articles from bookmarks query are bookmarked
                    pagingData.map { entity -> entity.toUiModel(isBookmarked = true) }
                }
            }
    }
}

/**
 * Exception thrown when repository operations fail.
 */
class NewsRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
