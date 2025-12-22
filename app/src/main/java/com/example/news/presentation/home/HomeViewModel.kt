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
 * ViewModel for HomeScreen.
 * Implements SSOT pattern: observes Room PagingData for articles, refreshes network in background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val repository: NewsRepository = NewsRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    /**
     * Get paged articles for current category and search query.
     * This Flow is reactive to category and search query changes via uiState StateFlow.
     * Cached in ViewModelScope for configuration changes.
     */
    val pagedArticles: Flow<PagingData<ArticleUiModel>> = _uiState
        .distinctUntilChanged { old, new -> 
            old.selectedCategory == new.selectedCategory && old.searchQuery == new.searchQuery
        }
        .flatMapLatest { state ->
            repository.getPagedArticles(state.selectedCategory, state.searchQuery)
                .cachedIn(viewModelScope)
        }
    
    private var currentCategory = "general"
    
    // Track the refresh job to cancel it when switching categories
    private var refreshJob: Job? = null
    
    init {
        // Start observing default category on init
        observeCategory("general")
    }
    
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
    
    private fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(articleId)
            // Bookmark state will automatically update via Flow when PagingSource invalidates
        }
    }
    
    /**
     * Observes articles for a category from Room (SSOT).
     * Also triggers background refresh.
     * The pagedArticles Flow will automatically update when selectedCategory changes.
     */
    private fun observeCategory(category: String) {
        // Cancel previous refresh job to prevent stale updates
        refreshJob?.cancel()
        
        currentCategory = category
        
        // Update selected category immediately
        // This will trigger pagedArticles Flow to switch to the new category
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isRefreshing = false,
            errorMessage = null
        )
        
        // Refresh from network in background (doesn't block UI)
        refreshCategory(category)
    }
    
    /**
     * Refreshes articles for current category from network.
     */
    private fun refreshCurrentCategory() {
        refreshCategory(currentCategory)
    }
    
    /**
     * Refreshes articles for a category from network.
     * Runs in background and updates Room, which automatically notifies PagingData observers.
     */
    private fun refreshCategory(category: String) {
        // Cancel previous refresh job
        refreshJob?.cancel()
        
        // Store the refresh job so we can cancel it if needed
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
