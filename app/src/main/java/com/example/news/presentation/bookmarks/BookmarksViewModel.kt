package com.example.news.presentation.bookmarks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.news.data.repository.NewsRepository
import com.example.news.data.repository.NewsRepositoryImpl
import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for the bookmarks screen.
 *
 * Provides a paginated stream of bookmarked articles sourced from the Room database.
 * When the user removes a bookmark, the underlying PagingSource is invalidated by the
 * repository, so the list automatically refreshes without manual intervention.
 *
 * @param application  The [Application] context required by [AndroidViewModel].
 * @param repository   The [NewsRepository] used for bookmark queries and mutations
 *                     (defaults to [NewsRepositoryImpl]).
 */
class BookmarksViewModel(
    application: Application,
    private val repository: NewsRepository = NewsRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {
    
    /**
     * Reactive stream of paged bookmarked articles from Room.
     *
     * Cached in [viewModelScope] so the data survives configuration changes (e.g., screen
     * rotation). The flow re-emits whenever the underlying bookmark table changes.
     */
    val pagedArticles: Flow<PagingData<ArticleUiModel>> = repository
        .getPagedBookmarkedArticles()
        .cachedIn(viewModelScope)
    
    /**
     * Central event handler for user interactions on the bookmarks screen.
     *
     * @param event The [BookmarksUiEvent] dispatched from the composable UI.
     */
    fun handleEvent(event: BookmarksUiEvent) {
        when (event) {
            is BookmarksUiEvent.OnBookmarkToggle -> {
                toggleBookmark(event.articleId)
            }
        }
    }
    
    /**
     * Removes or adds a bookmark for the specified article.
     *
     * The repository handles the toggle logic (insert vs. delete) and invalidates the
     * PagingSource so that the bookmarks list updates automatically.
     *
     * @param articleId Unique identifier of the article whose bookmark state should be toggled.
     */
    private fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(articleId)
        }
    }
}

