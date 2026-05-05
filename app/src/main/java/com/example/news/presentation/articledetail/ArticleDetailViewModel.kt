package com.example.news.presentation.articledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.news.domain.model.BookmarkToggleResult
import com.example.news.presentation.articledetail.ArticleSummaryState
import com.example.news.domain.model.CategoryFollowResult
import com.example.news.domain.usecase.AuthAwareToggleBookmarkUseCase
import com.example.news.domain.usecase.AuthAwareToggleFollowCategoryUseCase
import com.example.news.domain.usecase.GetArticleByIdUseCase
import com.example.news.domain.usecase.GetArticleSummaryUseCase
import com.example.news.domain.usecase.GetFollowedCategoriesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getArticleByIdUseCase: GetArticleByIdUseCase,
    private val authAwareToggleBookmarkUseCase: AuthAwareToggleBookmarkUseCase,
    private val authAwareToggleFollowCategoryUseCase: AuthAwareToggleFollowCategoryUseCase,
    private val getFollowedCategoriesUseCase: GetFollowedCategoriesUseCase,
    private val getArticleSummaryUseCase: GetArticleSummaryUseCase
) : ViewModel(), ArticleDetailScreenEvents {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])

    private val _uiState = MutableStateFlow(ArticleDetailUiState())

    private val _authRequiredChannel = Channel<String>(Channel.BUFFERED)
    val authRequiredForBookmark: Flow<String> = _authRequiredChannel.receiveAsFlow()

    private val _authRequiredForFollowChannel = Channel<String>(Channel.BUFFERED)
    val authRequiredForFollowCategory: Flow<String> = _authRequiredForFollowChannel.receiveAsFlow()

    private val _followSnackbarChannel = Channel<String>(Channel.BUFFERED)
    val followSnackbarMessage: Flow<String> = _followSnackbarChannel.receiveAsFlow()

    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        loadArticle()
    }

    override fun onBookmarkToggle() {
        toggleBookmark()
    }

    override fun onFollowCategoryToggle() {
        toggleFollowCategory()
    }

    private fun loadArticle() {
        viewModelScope.launch {
            try {
                val article = getArticleByIdUseCase(articleId)
                if (article != null) {
                    _uiState.value = ArticleDetailUiState(article = article, isLoading = false)
                    observeFollowedCategories(article.category)
                    fetchSummary(article.id, article.articleUrl, article.title)
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

    private fun observeFollowedCategories(articleCategory: String) {
        viewModelScope.launch {
            getFollowedCategoriesUseCase().collect { followed ->
                _uiState.value = _uiState.value.copy(
                    isCategoryFollowed = followed.contains(articleCategory)
                )
            }
        }
    }

    private fun fetchSummary(articleId: String, articleUrl: String, title: String) {
        _uiState.value = _uiState.value.copy(summaryState = ArticleSummaryState.Loading)
        viewModelScope.launch {
            getArticleSummaryUseCase(articleId, articleUrl, title)
                .onSuccess { points ->
                    _uiState.value = _uiState.value.copy(summaryState = ArticleSummaryState.Success(points))
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(summaryState = ArticleSummaryState.Error)
                }
        }
    }

    private fun toggleBookmark() {
        val currentArticle = _uiState.value.article ?: return
        viewModelScope.launch {
            val result = authAwareToggleBookmarkUseCase(articleId, currentArticle.isBookmarked)
            when (result) {
                is BookmarkToggleResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        article = currentArticle.copy(isBookmarked = !currentArticle.isBookmarked)
                    )
                }
                is BookmarkToggleResult.AuthRequired -> {
                    _authRequiredChannel.send(result.articleId)
                }
            }
        }
    }

    private fun toggleFollowCategory() {
        val currentArticle = _uiState.value.article ?: return
        val categoryId = currentArticle.category
        val isCurrentlyFollowed = _uiState.value.isCategoryFollowed
        viewModelScope.launch {
            val result = authAwareToggleFollowCategoryUseCase(categoryId, isCurrentlyFollowed)
            when (result) {
                is CategoryFollowResult.Success -> {
                    val message = if (isCurrentlyFollowed) {
                        "Unfollowed ${categoryId.replaceFirstChar { it.uppercase() }}"
                    } else {
                        "Following ${categoryId.replaceFirstChar { it.uppercase() }}"
                    }
                    _followSnackbarChannel.send(message)
                }
                is CategoryFollowResult.AuthRequired -> {
                    _authRequiredForFollowChannel.send(result.categoryId)
                }
            }
        }
    }
}
