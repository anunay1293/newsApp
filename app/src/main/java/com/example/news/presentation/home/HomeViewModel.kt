package com.example.news.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.news.domain.model.Article
import com.example.news.domain.model.BookmarkToggleResult
import com.example.news.domain.usecase.AuthAwareToggleBookmarkUseCase
import com.example.news.domain.usecase.GetPagedArticlesUseCase
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

/**
 * ViewModel for the home / news-feed screen.
 *
 * Implements the **Single Source of Truth (SSOT)** pattern via domain use cases:
 * - The UI always reads articles from the local Room database via Paging 3.
 * - Network requests are fired in the background to refresh Room; Room then notifies the
 *   PagingData observers automatically.
 *
 * Key responsibilities:
 * 1. Category selection -- switches the Room query and triggers a background refresh.
 * 2. Search -- filters the Room-backed PagingData by title, author, or source name.
 * 3. Bookmark toggling -- delegates to [ToggleBookmarkUseCase] and relies on PagingSource
 *    invalidation to refresh bookmark icons.
 * 4. Error / retry -- surfaces network errors and allows the user to retry a failed refresh.
 * 5. Pull-to-refresh -- user-initiated refresh via a swipe gesture, tracked by a dedicated
 *    [HomeUiState.isPullRefreshing] flag to avoid showing the pull indicator during
 *    automatic background refreshes.
 *
 * @param getPagedArticlesUseCase  Use case for observing paginated articles from Room.
 * @param refreshArticlesUseCase   Use case for fetching fresh articles from the remote API.
 * @param toggleBookmarkUseCase    Use case for toggling an article's bookmark state.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val getPagedArticlesUseCase: GetPagedArticlesUseCase,
    private val refreshArticlesUseCase: RefreshArticlesUseCase,
    private val authAwareToggleBookmarkUseCase: AuthAwareToggleBookmarkUseCase
) : ViewModel(), HomeScreenEvents {

    private val _uiState = MutableStateFlow(HomeUiState())

    private val _authRequiredChannel = Channel<String>(Channel.BUFFERED)
    val authRequiredForBookmark: Flow<String> = _authRequiredChannel.receiveAsFlow()

    /** Observable UI state consumed by the home screen composable. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Reactive stream of paged domain [Article] objects from the Room database.
     *
     * Re-emits a new [PagingData] whenever [HomeUiState.selectedCategory] or
     * [HomeUiState.searchQuery] changes, using [flatMapLatest] to cancel stale queries.
     * The resulting flow is cached in [viewModelScope] to survive configuration changes.
     * The UI layer is responsible for mapping each [Article] to an [ArticleUiModel] at
     * render time.
     */
    val pagedArticles: Flow<PagingData<Article>> = _uiState
        .distinctUntilChanged { old, new ->
            old.selectedCategory == new.selectedCategory && old.searchQuery == new.searchQuery
        }
        .flatMapLatest { state ->
            getPagedArticlesUseCase(state.selectedCategory, state.searchQuery)
                .cachedIn(viewModelScope)
        }

    private var currentCategory = "general"

    private var refreshJob: Job? = null

    init {
        observeCategory("general")
    }

    override fun onCategorySelected(category: String) {
        observeCategory(category)
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

    private fun toggleBookmark(articleId: String, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            val result = authAwareToggleBookmarkUseCase(articleId, isCurrentlyBookmarked)
            if (result is BookmarkToggleResult.AuthRequired) {
                _authRequiredChannel.send(result.articleId)
            }
        }
    }

    /**
     * Switches the active news category and begins observing articles for it.
     *
     * Steps performed:
     * 1. Cancel any in-flight network refresh for the previous category.
     * 2. Update [HomeUiState.selectedCategory], which triggers [pagedArticles] to re-query Room.
     * 3. Kick off a background network refresh so Room receives the latest articles.
     */
    private fun observeCategory(category: String) {
        refreshJob?.cancel()

        currentCategory = category

        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isRefreshing = false,
            errorMessage = null
        )

        refreshCategory(category)
    }

    private fun refreshCurrentCategory() {
        refreshCategory(currentCategory)
    }

    /**
     * Fetches fresh articles for [category] from the remote API via [RefreshArticlesUseCase]
     * and upserts them into Room.
     *
     * While the refresh is in progress, [HomeUiState.isRefreshing] is `true`. On failure
     * the error is surfaced via [HomeUiState.errorMessage]; the cached data in Room
     * remains available to the user.
     */
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

    /**
     * Handles a user-initiated pull-to-refresh gesture.
     *
     * Uses [HomeUiState.isPullRefreshing] instead of [HomeUiState.isRefreshing] so the
     * pull indicator is only driven by physical pull gestures, not by automatic background
     * refreshes (category change, initial load). The underlying API call and Room upsert
     * are identical to [refreshCategory].
     */
    private fun pullToRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPullRefreshing = true)
            try {
                refreshArticlesUseCase(currentCategory)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to refresh articles"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isPullRefreshing = false)
            }
        }
    }
}
