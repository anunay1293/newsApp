package com.example.news.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import android.util.Log
import com.example.news.data.api.BookmarkApiService
import com.example.news.data.api.NewsApiService
import com.example.news.data.dto.BookmarkDto
import com.example.news.data.local.ArticleDao
import com.example.news.data.local.ArticleEntity
import com.example.news.data.local.BookmarkDao
import com.example.news.data.local.BookmarkEntity
import com.example.news.data.mapper.toDomain
import com.example.news.data.mapper.toEntity
import com.example.news.domain.model.Article
import com.example.news.domain.repository.NewsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Concrete implementation of [NewsRepository] using the **Single Source of Truth (SSOT)** pattern.
 *
 * Architecture overview:
 * - **Reads** are served exclusively from Room via Paging 3, returning domain [Article] objects.
 * - **Writes** fetch data from the remote API, map DTOs to entities, and upsert them into Room.
 *   Room then automatically invalidates the PagingSource so observers pick up changes.
 * - A separate in-memory [_bookmarkedIds] cache tracks bookmark state without requiring
 *   a JOIN on every page load.
 *
 * All dependencies are provided by Hilt via constructor injection.
 *
 * @param articleDao     DAO for article CRUD and paging queries.
 * @param bookmarkDao    DAO for bookmark CRUD, observation, and paged bookmark queries.
 * @param newsApiService The Retrofit API service for fetching news articles from the remote API.
 */
private const val TAG = "NewsRepositoryImpl"

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NewsRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val bookmarkDao: BookmarkDao,
    private val newsApiService: NewsApiService,
    private val bookmarkApiService: BookmarkApiService
) : NewsRepository {
    
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
     * 1. Refreshes the in-memory bookmark-ID cache so bookmark state is accurate.
     * 2. Uses [flatMapLatest] on [_bookmarkedIds] so that a new [Pager] is created whenever
     *    the bookmark set changes, avoiding the "collect twice" error on the same Pager flow.
     * 3. Maps each [ArticleEntity] to a domain [Article], setting [isBookmarked] from the cache.
     *
     * Paging configuration: 20 items per page, 10-item prefetch distance, no placeholders.
     */
    override fun getPagedArticles(category: String, searchQuery: String): Flow<PagingData<Article>> {
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
                    entity.toDomain(isBookmarked = bookmarkedSet.contains(entity.articleId))
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
        val isNowBookmarked: Boolean
        if (existingBookmark != null) {
            bookmarkDao.deleteBookmark(existingBookmark)
            _bookmarkedIds.value = _bookmarkedIds.value - articleId
            isNowBookmarked = false
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(articleId = articleId))
            _bookmarkedIds.value = _bookmarkedIds.value + articleId
            isNowBookmarked = true
        }
        refreshBookmarkedIds()

        bookmarkScope.launch {
            try {
                syncBookmarkToRemote(articleId, isNowBookmarked)
            } catch (e: Exception) {
                Log.e(TAG, "Remote bookmark sync failed for $articleId", e)
            }
        }
    }
    
    /**
     * Retrieves a single article by its [articleId] from the local Room database.
     *
     * Bookmark state is resolved from the in-memory [_bookmarkedIds] cache so the returned
     * [Article] reflects the current bookmark status without an extra database query.
     *
     * @return The matching [Article] with its bookmark state, or `null` if not found.
     */
    override suspend fun getArticleById(articleId: String): Article? {
        val entity = articleDao.getArticleById(articleId) ?: return null
        val isBookmarked = _bookmarkedIds.value.contains(articleId)
        return entity.toDomain(isBookmarked)
    }

    /**
     * Returns a paginated stream of all bookmarked articles.
     *
     * Observes [bookmarkedIdsFromRoom] and uses [flatMapLatest] to create a fresh [Pager]
     * each time the bookmark table changes. Every entity returned by the bookmark JOIN query
     * is mapped with `isBookmarked = true` since, by definition, all results are bookmarked.
     */
    override fun getPagedBookmarkedArticles(): Flow<PagingData<Article>> {
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
                    pagingData.map { entity -> entity.toDomain(isBookmarked = true) }
                }
            }
    }

    override suspend fun syncBookmarksFromRemote() {
        try {
            val response = bookmarkApiService.getBookmarks()
            val remoteBookmarks = response.bookmarks

            for (dto in remoteBookmarks) {
                val entity = ArticleEntity(
                    articleId = dto.articleId,
                    category = dto.category,
                    title = dto.title,
                    author = dto.author,
                    publishedAt = dto.publishedAt,
                    url = dto.url,
                    urlToImage = dto.urlToImage,
                    sourceName = dto.sourceName,
                    fetchedAt = System.currentTimeMillis()
                )
                articleDao.upsertArticles(listOf(entity))
            }

            bookmarkDao.deleteAllBookmarks()
            for (dto in remoteBookmarks) {
                bookmarkDao.insertBookmark(
                    BookmarkEntity(articleId = dto.articleId, bookmarkedAt = dto.bookmarkedAt)
                )
            }

            refreshBookmarkedIds()
            Log.d(TAG, "Synced ${remoteBookmarks.size} bookmarks from remote")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync bookmarks from remote", e)
        }
    }

    /**
     * Deletes all rows from the local bookmarks table and resets [_bookmarkedIds] to an
     * empty set. The empty set triggers [flatMapLatest] in [getPagedArticles], causing
     * the feed to re-emit all articles with `isBookmarked = false`.
     */
    override suspend fun clearLocalBookmarks() {
        bookmarkDao.deleteAllBookmarks()
        refreshBookmarkedIds()
    }

    override suspend fun syncBookmarkToRemote(articleId: String, isBookmarked: Boolean) {
        if (isBookmarked) {
            val entity = articleDao.getArticleById(articleId) ?: return
            val bookmark = bookmarkDao.getBookmark(articleId)
            val dto = BookmarkDto(
                articleId = entity.articleId,
                bookmarkedAt = bookmark?.bookmarkedAt ?: System.currentTimeMillis(),
                title = entity.title,
                author = entity.author,
                publishedAt = entity.publishedAt,
                url = entity.url,
                urlToImage = entity.urlToImage,
                sourceName = entity.sourceName,
                category = entity.category
            )
            bookmarkApiService.addBookmark(dto)
        } else {
            bookmarkApiService.removeBookmark(articleId)
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
