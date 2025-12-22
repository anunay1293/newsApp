package com.example.news.ui.model

/**
 * UI model for article data displayed in the HomeScreen.
 * This is a UI-only model and will be populated from domain/data layer later.
 */
data class ArticleUiModel(
    val id: String,
    val title: String,
    val author: String?,
    val publishedDate: Long, // Unix timestamp in milliseconds
    val imageUrl: String?,
    val articleUrl: String,
    val isBookmarked: Boolean = false
)

