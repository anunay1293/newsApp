package com.example.news.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.news.data.repository.NewsRepository
import com.example.news.data.repository.NewsRepositoryImpl
import com.example.news.ui.model.ArticleUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel for the home / news-feed screen.
 *
 * Implements the **Single Source of Truth (SSOT)** pattern:
 * - The UI always reads articles from the local Room database via Paging 3.
 * - Network requests are fired in the background to refresh Room; Room then notifies the
 *   PagingData observers automatically.
 *
 * Key responsibilities:
 * 1. Category selection – switches the Room query and triggers a background refresh.
 * 2. Search – filters the Room-backed PagingData by title, author, or source name.
 * 3. Bookmark toggling – delegates to the repository and relies on PagingSource invalidation
 *    to refresh bookmark icons.
 * 4. Error / retry – surfaces network errors and allows the user to retry a failed refresh.
 *
 * @param application  The [Application] context needed by [AndroidViewModel].
 * @param repository   The [NewsRepository] used for data operations (defaults to [NewsRepositoryImpl]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val repository: NewsRepository = NewsRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {
    
    /** Mutable backing field for the non-paging UI state (category, search query, refreshing, errors). */
    private val _uiState = MutableStateFlow(HomeUiState())

    /** Observable UI state consumed by the home screen composable. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    /**
     * Reactive stream of paged articles from the Room database.
     *
     * Re-emits a new [PagingData] whenever [HomeUiState.selectedCategory] or
     * [HomeUiState.searchQuery] changes, using [flatMapLatest] to cancel stale queries.
     * The resulting flow is cached in [viewModelScope] to survive configuration changes.
     */
    val pagedArticles: Flow<PagingData<ArticleUiModel>> = _uiState
        .distinctUntilChanged { old, new -> 
            old.selectedCategory == new.selectedCategory && old.searchQuery == new.searchQuery
        }
        .flatMapLatest { state ->
            repository.getPagedArticles(state.selectedCategory, state.searchQuery)
                .cachedIn(viewModelScope)
        }
    
    /** The currently selected news category (e.g., "general", "technology"). */
    private var currentCategory = "general"
    
    /** Coroutine [Job] for the current network refresh; cancelled when switching categories. */
    private var refreshJob: Job? = null
    
    init {
        observeCategory("general")
    }
    
    /**
     * Central event handler for all user interactions on the home screen.
     *
     * Dispatches each [HomeUiEvent] to the appropriate internal handler method, keeping
     * the composable layer free of business logic.
     *
     * @param event The UI event raised by user interaction.
     */
    fun handleEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnCategorySelected -> {
                observeCategory(event.category)
            }
            is HomeUiEvent.OnSearchQueryChanged -> {
                // Update search query in state - this will trigger pagedArticles Flow update
                _uiState.value = _uiState.value.copy(searchQuery = event.searchQuery)
            }
            is HomeUiEvent.OnBookmarkToggle -> {
                toggleBookmark(event.articleId)
            }
            is HomeUiEvent.OnRetryClicked -> {
                refreshCurrentCategory()
            }
        }
    }
    
    /**
     * Toggles the bookmark state for the article identified by [articleId].
     *
     * The actual bookmark mutation is performed by the repository. Because the PagingSource
     * is invalidated on bookmark changes, the UI list will automatically reflect the update.
     *
     * @param articleId Unique identifier of the article to bookmark or un-bookmark.
     */
    private fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(articleId)
        }
    }
    
    /**
     * Switches the active news category and begins observing articles for it.
     *
     * Steps performed:
     * 1. Cancel any in-flight network refresh for the previous category.
     * 2. Update [HomeUiState.selectedCategory], which triggers [pagedArticles] to re-query Room.
     * 3. Kick off a background network refresh so Room receives the latest articles.
     *
     * @param category The news category to switch to (e.g., "technology", "sports").
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
    
    /**
     * Convenience wrapper that refreshes articles for the currently selected category.
     * Called when the user taps the "Retry" button after a network error.
     */
    private fun refreshCurrentCategory() {
        refreshCategory(currentCategory)
    }
    
    /**
     * Fetches fresh articles for [category] from the remote API and upserts them into Room.
     *
     * The refresh runs on a cancellable coroutine ([refreshJob]) so that switching categories
     * can cancel a stale request. While the refresh is in progress, [HomeUiState.isRefreshing]
     * is `true`. On failure the error is surfaced via [HomeUiState.errorMessage]; the cached
     * data in Room remains available to the user.
     *
     * @param category The news category to refresh (e.g., "general").
     */
    private fun refreshCategory(category: String) {
        refreshJob?.cancel()
        
        refreshJob = viewModelScope.launch {
            try {
                // Set refreshing state (non-blocking indicator)
                if (currentCategory == category) {
                    _uiState.value = _uiState.value.copy(isRefreshing = true)
                }
                
                repository.refreshArticles(category)
                
                // Refreshing will be turned off after a short delay or when PagingData updates
                // Room updates will automatically trigger PagingSource invalidation
                
            } catch (e: Exception) {
                // This shouldn't happen as repository doesn't throw on refresh error
                // But handle it just in case
                // Only update error if this is still the current category
                if (currentCategory == category) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = e.message ?: "Failed to refresh articles"
                    )
                }
            } finally {
                // Turn off refreshing indicator after refresh completes
                if (currentCategory == category) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            }
        }
    }
}
