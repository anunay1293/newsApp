package com.example.news.presentation.bookmarks

/**
 * UI events sent from BookmarksScreen to ViewModel.
 */
sealed class BookmarksUiEvent {
    data class OnBookmarkToggle(val articleId: String) : BookmarksUiEvent()
}

