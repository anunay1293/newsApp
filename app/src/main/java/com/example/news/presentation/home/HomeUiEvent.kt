package com.example.news.presentation.home

/**
 * Sealed class representing all user-initiated events on the home screen.
 *
 * Events flow **upward** from the composable UI to [HomeViewModel.handleEvent], which
 * interprets each event and updates the state accordingly. This unidirectional data-flow
 * pattern keeps the composable layer stateless and testable.
 */
sealed class HomeUiEvent {

    /**
     * The user selected a different news category from the dropdown.
     *
     * @property category The chosen category identifier (e.g., "technology", "sports").
     */
    data class OnCategorySelected(val category: String) : HomeUiEvent()

    /**
     * The user typed or cleared text in the search field.
     *
     * @property searchQuery The current contents of the search input; may be empty.
     */
    data class OnSearchQueryChanged(val searchQuery: String) : HomeUiEvent()

    /**
     * The user tapped the bookmark icon on an article card.
     *
     * @property articleId The unique identifier of the article to bookmark or un-bookmark.
     */
    data class OnBookmarkToggle(val articleId: String) : HomeUiEvent()

    /** The user tapped the "Retry" button after a network error to re-attempt the refresh. */
    object OnRetryClicked : HomeUiEvent()
}

