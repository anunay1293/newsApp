package com.example.news.presentation.bookmarks

/**
 * Sealed class representing user-initiated events on the bookmarks screen.
 *
 * Events are dispatched from the composable UI to [BookmarksViewModel.handleEvent],
 * following the unidirectional data-flow pattern.
 */
sealed class BookmarksUiEvent {

    /**
     * The user tapped the bookmark icon on a bookmarked article card, requesting removal
     * (or re-addition) of the bookmark.
     *
     * @property articleId Unique identifier of the affected article.
     */
    data class OnBookmarkToggle(val articleId: String) : BookmarksUiEvent()
}

