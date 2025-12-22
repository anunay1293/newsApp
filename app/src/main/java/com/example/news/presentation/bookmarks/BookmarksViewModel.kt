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
 * ViewModel for BookmarksScreen.
 * Observes bookmarked articles from Room via Paging 3.
 */
class BookmarksViewModel(
    application: Application,
    private val repository: NewsRepository = NewsRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {
    
    /**
     * Get paged bookmarked articles from Room.
     * Cached in ViewModelScope for configuration changes.
     */
    val pagedArticles: Flow<PagingData<ArticleUiModel>> = repository
        .getPagedBookmarkedArticles()
        .cachedIn(viewModelScope)
    
    fun handleEvent(event: BookmarksUiEvent) {
        when (event) {
            is BookmarksUiEvent.OnBookmarkToggle -> {
                toggleBookmark(event.articleId)
            }
        }
    }
    
    private fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(articleId)
            // Bookmark state will automatically update via Flow when PagingSource invalidates
        }
    }
}

