package com.example.news.presentation.home

/**
 * UI state for HomeScreen.
 * Articles are handled separately via PagingData Flow.
 */
data class HomeUiState(
    val selectedCategory: String = "general",
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

