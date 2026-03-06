package com.example.news.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.news.domain.model.Article
import com.example.news.domain.repository.AuthRepository
import com.example.news.domain.usecase.GetPagedBookmarkedArticlesUseCase
import com.example.news.domain.usecase.ToggleBookmarkUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val authRepository: AuthRepository
) : ViewModel(), BookmarksScreenEvents {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    val pagedArticles: Flow<PagingData<Article>> = getPagedBookmarkedArticlesUseCase()
        .cachedIn(viewModelScope)

    init {
        onRecheckAuth()
    }

    override fun onRecheckAuth() {
        viewModelScope.launch {
            _isSignedIn.value = authRepository.checkSession()
        }
    }

    override fun onBookmarkToggle(articleId: String) {
        viewModelScope.launch {
            toggleBookmarkUseCase(articleId)
        }
    }
}
