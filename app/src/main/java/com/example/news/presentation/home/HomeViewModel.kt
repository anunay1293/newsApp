package com.example.news.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.news.domain.model.Article
import com.example.news.domain.model.BookmarkToggleResult
import com.example.news.domain.model.CategoryFollowResult
import com.example.news.domain.usecase.AuthAwareToggleBookmarkUseCase
import com.example.news.domain.usecase.AuthAwareToggleFollowCategoryUseCase
import com.example.news.domain.usecase.CheckAuthSessionUseCase
import com.example.news.domain.usecase.GetFollowedCategoriesUseCase
import com.example.news.domain.usecase.GetPagedArticlesUseCase
import com.example.news.domain.usecase.GetPagedFollowedArticlesUseCase
import com.example.news.domain.usecase.RefreshArticlesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val getPagedArticlesUseCase: GetPagedArticlesUseCase,
    private val refreshArticlesUseCase: RefreshArticlesUseCase,
    private val authAwareToggleBookmarkUseCase: AuthAwareToggleBookmarkUseCase,
    private val authAwareToggleFollowCategoryUseCase: AuthAwareToggleFollowCategoryUseCase,
    private val getPagedFollowedArticlesUseCase: GetPagedFollowedArticlesUseCase,
    private val getFollowedCategoriesUseCase: GetFollowedCategoriesUseCase,
    private val checkAuthSessionUseCase: CheckAuthSessionUseCase
) : ViewModel(), HomeScreenEvents {

    private val _uiState = MutableStateFlow(HomeUiState())

    private val _authRequiredChannel = Channel<String>(Channel.BUFFERED)
    val authRequiredForBookmark: Flow<String> = _authRequiredChannel.receiveAsFlow()

    private val _authRequiredForFollowChannel = Channel<String>(Channel.BUFFERED)
    val authRequiredForFollowCategory: Flow<String> = _authRequiredForFollowChannel.receiveAsFlow()

    private val _followSnackbarChannel = Channel<String>(Channel.BUFFERED)
    val followSnackbarMessage: Flow<String> = _followSnackbarChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val pagedArticles: Flow<PagingData<Article>> = _uiState
        .distinctUntilChanged { old, new ->
            old.selectedCategory == new.selectedCategory && old.searchQuery == new.searchQuery
        }
        .flatMapLatest { state ->
            if (state.selectedCategory == FOLLOWED_CATEGORY) {
                getPagedFollowedArticlesUseCase(state.searchQuery).cachedIn(viewModelScope)
            } else {
                getPagedArticlesUseCase(state.selectedCategory, state.searchQuery)
                    .cachedIn(viewModelScope)
            }
        }

    private var currentCategory = "general"
    private var refreshJob: Job? = null

    init {
        observeCategory("general")
        viewModelScope.launch {
            getFollowedCategoriesUseCase().collect { followed ->
                _uiState.value = _uiState.value.copy(
                    followedCategories = followed,
                    isCurrentCategoryFollowed = followed.contains(_uiState.value.selectedCategory)
                )
            }
        }
    }

    override fun onCategorySelected(category: String) {
        if (category == FOLLOWED_CATEGORY) {
            viewModelScope.launch {
                if (!checkAuthSessionUseCase()) {
                    _authRequiredForFollowChannel.send(FOLLOWED_CATEGORY)
                } else {
                    observeFollowedFeed()
                }
            }
        } else {
            observeCategory(category)
        }
    }

    override fun onSearchQueryChanged(searchQuery: String) {
        _uiState.value = _uiState.value.copy(searchQuery = searchQuery)
    }

    override fun onBookmarkToggle(articleId: String, isCurrentlyBookmarked: Boolean) {
        toggleBookmark(articleId, isCurrentlyBookmarked)
    }

    override fun onRetryClicked() {
        refreshCurrentCategory()
    }

    override fun onPullToRefresh() {
        pullToRefresh()
    }

    override fun onFollowCategoryToggle(categoryId: String, isCurrentlyFollowed: Boolean) {
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

    private fun toggleBookmark(articleId: String, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            val result = authAwareToggleBookmarkUseCase(articleId, isCurrentlyBookmarked)
            if (result is BookmarkToggleResult.AuthRequired) {
                _authRequiredChannel.send(result.articleId)
            }
        }
    }

    private fun observeCategory(category: String) {
        refreshJob?.cancel()
        currentCategory = category
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isRefreshing = false,
            errorMessage = null,
            isCurrentCategoryFollowed = _uiState.value.followedCategories.contains(category)
        )
        refreshCategory(category)
    }

    private fun observeFollowedFeed() {
        refreshJob?.cancel()
        currentCategory = FOLLOWED_CATEGORY
        _uiState.value = _uiState.value.copy(
            selectedCategory = FOLLOWED_CATEGORY,
            isRefreshing = false,
            errorMessage = null,
            isCurrentCategoryFollowed = false
        )
        refreshFollowedCategories()
    }

    private fun refreshFollowedCategories() {
        val followed = _uiState.value.followedCategories
        if (followed.isEmpty()) return
        refreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                followed.forEach { category ->
                    launch { refreshArticlesUseCase(category) }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to refresh followed categories"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun refreshCurrentCategory() {
        if (currentCategory == FOLLOWED_CATEGORY) {
            refreshFollowedCategories()
        } else {
            refreshCategory(currentCategory)
        }
    }

    private fun refreshCategory(category: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                if (currentCategory == category) {
                    _uiState.value = _uiState.value.copy(isRefreshing = true)
                }
                refreshArticlesUseCase(category)
            } catch (e: Exception) {
                if (currentCategory == category) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = e.message ?: "Failed to refresh articles"
                    )
                }
            } finally {
                if (currentCategory == category) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            }
        }
    }

    private fun pullToRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPullRefreshing = true)
            try {
                if (currentCategory == FOLLOWED_CATEGORY) {
                    _uiState.value.followedCategories.forEach { category ->
                        launch { refreshArticlesUseCase(category) }
                    }
                } else {
                    refreshArticlesUseCase(currentCategory)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to refresh articles"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isPullRefreshing = false)
            }
        }
    }

    companion object {
        const val FOLLOWED_CATEGORY = "followed"
    }
}
