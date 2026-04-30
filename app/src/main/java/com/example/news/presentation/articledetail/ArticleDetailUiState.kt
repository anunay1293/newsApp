package com.example.news.presentation.articledetail

import com.example.news.domain.model.Article

/**
 * Immutable data class representing the article detail screen's UI state.
 *
 * Holds the domain [Article] rather than a UI model so the presentation layer remains
 * independent of the UI layer. The composable maps [Article] to [ArticleUiModel] at
 * render time.
 *
 * The screen transitions through three visual states:
 * 1. **Loading** ([isLoading] = `true`) — a progress indicator is shown while
 *    the article is fetched from Room.
 * 2. **Content** ([article] is non-null) — the TopAppBar and WebView are rendered.
 * 3. **Error** ([errorMessage] is non-null) — an error message replaces the content.
 *
 * @property article      The domain article to display, or `null` while loading / on error.
 * @property isLoading    `true` while the article is being loaded from the local database.
 * @property errorMessage A user-facing error description, or `null` when there is no error.
 */
data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isCategoryFollowed: Boolean = false
)
