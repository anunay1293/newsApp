package com.example.news.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.news.domain.model.Article
import com.example.news.domain.usecase.GetPagedBookmarkedArticlesUseCase
import com.example.news.domain.usecase.ToggleBookmarkUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for the bookmarks screen.
 *
 * Provides a paginated stream of bookmarked articles sourced from the Room database
 * via [GetPagedBookmarkedArticlesUseCase]. When the user removes a bookmark, the
 * underlying PagingSource is invalidated by the repository, so the list automatically
 * refreshes without manual intervention.
 *
 * @param getPagedBookmarkedArticlesUseCase Use case for observing paginated bookmarks from Room.
 * @param toggleBookmarkUseCase            Use case for toggling an article's bookmark state.
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val getPagedBookmarkedArticlesUseCase: GetPagedBookmarkedArticlesUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase
) : ViewModel() {

    /**
     * Reactive stream of paged bookmarked domain [Article] objects from Room.
     *
     * Cached in [viewModelScope] so the data survives configuration changes. The flow
     * re-emits whenever the underlying bookmark table changes. The UI layer is
     * responsible for mapping each [Article] to an [ArticleUiModel] at render time.
     */
    val pagedArticles: Flow<PagingData<Article>> = getPagedBookmarkedArticlesUseCase()
        .cachedIn(viewModelScope)

    /**
     * Central event handler for user interactions on the bookmarks screen.
     */
    fun handleEvent(event: BookmarksUiEvent) {
        when (event) {
            is BookmarksUiEvent.OnBookmarkToggle -> {
                toggleBookmark(event.articleId)
            }
        }
    }

    private fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            toggleBookmarkUseCase(articleId)
        }
    }
}
