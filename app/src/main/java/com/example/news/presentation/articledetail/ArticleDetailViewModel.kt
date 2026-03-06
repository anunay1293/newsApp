package com.example.news.presentation.articledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.news.domain.usecase.GetArticleByIdUseCase
import com.example.news.domain.usecase.ToggleBookmarkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the article detail screen.
 *
 * Loads a single article from the local Room database using the `articleId` navigation
 * argument (injected via [SavedStateHandle]) and exposes it as observable UI state.
 * The user can toggle the article's bookmark status; the change is applied optimistically
 * in the UI state for instant feedback while the repository persists it to Room.
 *
 * @param savedStateHandle  Handle providing navigation arguments; must contain "articleId".
 * @param getArticleByIdUseCase Use case for fetching a single article by its ID.
 * @param toggleBookmarkUseCase Use case for toggling an article's bookmark state.
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getArticleByIdUseCase: GetArticleByIdUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])

    private val _uiState = MutableStateFlow(ArticleDetailUiState())

    /** Observable UI state consumed by the article detail screen composable. */
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        loadArticle()
    }

    /**
     * Central event handler for all user interactions on the article detail screen.
     *
     * Dispatches each [ArticleDetailUiEvent] to the appropriate internal handler,
     * keeping the composable layer free of business logic.
     */
    fun handleEvent(event: ArticleDetailUiEvent) {
        when (event) {
            is ArticleDetailUiEvent.OnBookmarkToggle -> toggleBookmark()
        }
    }

    /**
     * Fetches the article from the local database and updates [_uiState].
     * If the article is not found (e.g., it was pruned from the cache), an error
     * message is surfaced instead.
     */
    private fun loadArticle() {
        viewModelScope.launch {
            try {
                val article = getArticleByIdUseCase(articleId)
                if (article != null) {
                    _uiState.value = ArticleDetailUiState(
                        article = article,
                        isLoading = false
                    )
                } else {
                    _uiState.value = ArticleDetailUiState(
                        isLoading = false,
                        errorMessage = "Article not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ArticleDetailUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load article"
                )
            }
        }
    }

    /**
     * Toggles the bookmark state for the current article.
     *
     * The UI state is updated **optimistically** (the heart icon flips immediately)
     * while [ToggleBookmarkUseCase] persists the change to Room in the background.
     */
    private fun toggleBookmark() {
        val currentArticle = _uiState.value.article ?: return
        _uiState.value = _uiState.value.copy(
            article = currentArticle.copy(isBookmarked = !currentArticle.isBookmarked)
        )
        viewModelScope.launch {
            toggleBookmarkUseCase(articleId)
        }
    }
}
