package com.example.news.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.news.data.repository.NewsRepository
import com.example.news.data.repository.NewsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for HomeScreen.
 * Implements SSOT pattern: observes Room Flow for articles, refreshes network in background.
 */
class HomeViewModel(
    application: Application,
    private val repository: NewsRepository = NewsRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var currentCategory = "general"
    
    init {
        // Start observing default category on init
        observeCategory("general")
    }
    
    fun handleEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnCategorySelected -> {
                observeCategory(event.category)
            }
            is HomeUiEvent.OnRetryClicked -> {
                refreshCurrentCategory()
            }
        }
    }
    
    /**
     * Observes articles for a category from Room (SSOT).
     * Also triggers background refresh.
     */
    private fun observeCategory(category: String) {
        currentCategory = category
        
        // Update selected category immediately
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isRefreshing = false,
            errorMessage = null
        )
        
        // Observe Room Flow - this is the single source of truth
        repository.observeArticles(category)
            .catch { e ->
                // Handle Flow errors
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load articles",
                    isRefreshing = false
                )
            }
            .onEach { articles ->
                // Update UI state from Room
                _uiState.value = _uiState.value.copy(
                    articles = articles,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
            .launchIn(viewModelScope)
        
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
     * Runs in background and updates Room, which automatically notifies Flow observers.
     */
    private fun refreshCategory(category: String) {
        viewModelScope.launch {
            try {
                // Set refreshing state only if we have cached data
                // If no cached data, loading will be handled by empty state
                if (_uiState.value.articles.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(isRefreshing = true)
                }
                
                repository.refreshArticles(category)
                
                // Refreshing will be turned off when Flow emits new data
                // If error occurred, cached data remains visible (repository doesn't throw)
                
            } catch (e: Exception) {
                // This shouldn't happen as repository doesn't throw on refresh error
                // But handle it just in case
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    errorMessage = e.message ?: "Failed to refresh articles"
                )
            }
        }
    }
}
