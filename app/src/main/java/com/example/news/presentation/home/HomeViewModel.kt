package com.example.news.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.news.data.repository.NewsRepository
import com.example.news.data.repository.NewsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for HomeScreen.
 * Manages state and handles UI events.
 */
class HomeViewModel(
    private val repository: NewsRepository = NewsRepositoryImpl()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        // Load default category on init
        loadArticlesForCategory("general")
    }
    
    fun handleEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnCategorySelected -> {
                loadArticlesForCategory(event.category)
            }
            is HomeUiEvent.OnRetryClicked -> {
                loadArticlesForCategory(_uiState.value.selectedCategory)
            }
        }
    }
    
    private fun loadArticlesForCategory(category: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                isLoading = true,
                errorMessage = null
            )
            
            try {
                val articles = repository.fetchTopHeadlinesByCategory(category)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    articles = articles,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    articles = emptyList(),
                    errorMessage = e.message ?: "Failed to load articles"
                )
            }
        }
    }
}

