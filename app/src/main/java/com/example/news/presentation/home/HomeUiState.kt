package com.example.news.presentation.home

import com.example.news.ui.model.ArticleUiModel

/**
 * UI state for HomeScreen.
 */
data class HomeUiState(
    val selectedCategory: String = "general",
    val isRefreshing: Boolean = false,
    val articles: List<ArticleUiModel> = emptyList(),
    val errorMessage: String? = null
)

