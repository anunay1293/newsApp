package com.example.news.presentation.articledetail

import com.example.news.ui.model.ArticleUiModel

/**
 * Immutable data class representing the article detail screen's UI state.
 *
 * The screen transitions through three visual states:
 * 1. **Loading** ([isLoading] = `true`) — a progress indicator is shown while
 *    the article is fetched from Room.
 * 2. **Content** ([article] is non-null) — the TopAppBar and WebView are rendered.
 * 3. **Error** ([errorMessage] is non-null) — an error message replaces the content.
 *
 * @property article      The article to display, or `null` while loading / on error.
 * @property isLoading    `true` while the article is being loaded from the local database.
 * @property errorMessage A user-facing error description, or `null` when there is no error.
 */
data class ArticleDetailUiState(
    val article: ArticleUiModel? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
