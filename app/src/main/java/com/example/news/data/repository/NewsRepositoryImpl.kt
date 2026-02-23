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
 * Concrete implementation of [NewsRepository] using the **Single Source of Truth (SSOT)** pattern.
 *
 * Architecture overview:
 * - **Reads** are served exclusively from Room via Paging 3 ([ArticleDao], [BookmarkDao]).
 * - **Writes** fetch data from [NewsApiService], map DTOs to entities, and upsert them into Room.
 *   Room then automatically invalidates the PagingSource so the UI picks up changes.
 * - A separate in-memory [_bookmarkedIds] cache is maintained for the feed screen's bookmark
 *   icons, avoiding a JOIN on every page load.
 *
 * @param context        Application [Context] used to obtain the Room database singleton.
 * @param newsApiService The Retrofit API service for fetching news articles from the remote API
 *                       (defaults to the singleton from [NewsApiModule]).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NewsRepositoryImpl(
    context: Context,
    private val newsApiService: com.example.news.data.api.NewsApiService = NewsApiModule.newsApiService
) : NewsRepository {
    
    /** Singleton Room database instance. */
    private val database = NewsDatabase.getDatabase(context)

    /** DAO for article CRUD and paging queries. */
    private val articleDao = database.articleDao()

    /** DAO for bookmark CRUD, observation, and paged bookmark queries. */
    private val bookmarkDao = database.bookmarkDao()
    
    /** Dedicated IO-bound coroutine scope for bookmark cache refreshes. */
    private val bookmarkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * In-memory cache of bookmarked article IDs, used by [getPagedArticles] to set the
     * [ArticleUiModel.isBookmarked] flag without a database JOIN on every page.
     */
    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    
    init {
        bookmarkScope.launch {
            refreshBookmarkedIds()
        }
    }
    
    /**
     * Queries all bookmarked article IDs from Room and refreshes the [_bookmarkedIds] cache.
     * Called on init and after every bookmark toggle to keep the in-memory set current.
     */
    private suspend fun refreshBookmarkedIds() {
        val bookmarkedIdsList = bookmarkDao.getAllBookmarkedIds()
        _bookmarkedIds.value = bookmarkedIdsList.toSet()
    }
    
    /**
     * Reactive flow that observes the bookmark table directly via Room.
     *
     * Used by [getPagedBookmarkedArticles] to trigger a new Pager whenever bookmarks
     * change, ensuring the bookmarks screen always reflects the latest state regardless
     * of which repository instance performed the mutation.
     */
    private val bookmarkedIdsFromRoom: Flow<Set<String>> = bookmarkDao.observeAllBookmarkedIds()
        .map { it.toSet() }
        .distinctUntilChanged()
    
    /**
     * Returns a paginated article stream for the given [category] and optional [searchQuery].
     *
     * Implementation details:
     * 1. Refreshes the in-memory bookmark-ID cache so bookmark icons are accurate.
     * 2. Uses [flatMapLatest] on [_bookmarkedIds] so that a new [Pager] is created whenever
     *    the bookmark set changes, avoiding the "collect twice" error on the same Pager flow.
     * 3. Maps each [ArticleEntity] to an [ArticleUiModel], setting [isBookmarked] from the cache.
     *
     * Paging configuration: 20 items per page, 10-item prefetch distance, no placeholders.
     */
    override fun getPagedArticles(category: String, searchQuery: String): Flow<PagingData<ArticleUiModel>> {
        bookmarkScope.launch {
            refreshBookmarkedIds()
        }
        
        return _bookmarkedIds.flatMapLatest { bookmarkedSet ->
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
    
    /**
     * Fetches the latest articles for [category] from the remote API and persists them to Room.
     *
     * Steps:
     * 1. Call [NewsApiService.getFeed] for the requested category.
     * 2. Map each [ArticleDto] to an [ArticleEntity], tagging it with [category].
     * 3. Upsert into Room (insert-or-replace by primary key).
     * 4. Prune stale articles — keep the 100 most recent per category, but **never** delete
     *    bookmarked articles regardless of age.
     *
     * On any network or parsing failure the exception is caught silently so the UI continues
     * to display cached data, preserving the SSOT guarantee.
     */
    override suspend fun refreshArticles(category: String) {
        try {
            val response = newsApiService.getFeed(category = category)
            val articles = response.articles ?: emptyList()
            
            val entities = articles.map { articleDto ->
                articleDto.toEntity(category = category)
            }
            
            articleDao.upsertArticles(entities)
            articleDao.deleteOldNonBookmarkedArticles(category, keepLimit = 100)
            
        } catch (e: Exception) {
            // Silently swallow – cached data remains available via Room.
        }
    }
    
    /**
     * Toggles the bookmark for the article identified by [articleId].
     *
     * If a [BookmarkEntity] already exists for this ID it is deleted (un-bookmark);
     * otherwise a new entity is inserted (bookmark). The in-memory [_bookmarkedIds]
     * cache is updated **optimistically** for instant UI feedback, then reconciled
     * with Room to guarantee consistency.
     */
    override suspend fun toggleBookmark(articleId: String) {
        val existingBookmark = bookmarkDao.getBookmark(articleId)
        if (existingBookmark != null) {
            bookmarkDao.deleteBookmark(existingBookmark)
            _bookmarkedIds.value = _bookmarkedIds.value - articleId
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(articleId = articleId))
            _bookmarkedIds.value = _bookmarkedIds.value + articleId
        }
        refreshBookmarkedIds()
    }
    
    /**
     * Returns a paginated stream of all bookmarked articles.
     *
     * Observes [bookmarkedIdsFromRoom] and uses [flatMapLatest] to create a fresh [Pager]
     * each time the bookmark table changes. Every entity returned by the bookmark JOIN query
     * is mapped with `isBookmarked = true` since, by definition, all results are bookmarked.
     */
    override fun getPagedBookmarkedArticles(): Flow<PagingData<ArticleUiModel>> {
        return bookmarkedIdsFromRoom
            .flatMapLatest {
                val pagingSourceFactory = { bookmarkDao.getPagedBookmarkedArticles() }
                
                Pager(
                    config = PagingConfig(
                        pageSize = 20,
                        enablePlaceholders = false,
                        prefetchDistance = 10
                    ),
                    pagingSourceFactory = pagingSourceFactory
                ).flow.map { pagingData -> 
                    pagingData.map { entity -> entity.toUiModel(isBookmarked = true) }
                }
            }
    }
}

/**
 * Custom exception type for repository-level failures.
 *
 * Currently unused at runtime (errors are swallowed to preserve the SSOT guarantee),
 * but available for future use in cases where callers need to distinguish repository
 * errors from other exception types.
 *
 * @param message Human-readable description of the failure.
 * @param cause   Optional underlying throwable that triggered this exception.
 */
class NewsRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
