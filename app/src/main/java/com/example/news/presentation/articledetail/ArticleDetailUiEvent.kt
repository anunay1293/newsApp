package com.example.news.presentation.articledetail

/**
 * Sealed class representing user-initiated events on the article detail screen.
 *
 * Events are dispatched from the composable UI to [ArticleDetailViewModel.handleEvent],
 * following the unidirectional data-flow pattern used throughout the app.
 */
sealed class ArticleDetailUiEvent {

    /**
     * The user tapped the bookmark icon in the detail screen's TopAppBar,
     * requesting a toggle of the article's bookmark state.
     */
    object OnBookmarkToggle : ArticleDetailUiEvent()
}
