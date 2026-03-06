package com.example.news.presentation.home

/**
 * Immutable data class representing the non-paging portion of the home screen's UI state.
 *
 * Article list data is streamed separately via a `Flow<PagingData<ArticleUiModel>>` in
 * [HomeViewModel.pagedArticles] to leverage Paging 3's built-in load-state management.
 * This class captures everything else the home screen needs to render correctly.
 *
 * @property selectedCategory  The currently active news category filter (e.g., "general", "sports").
 * @property searchQuery       Free-text search term applied against title, author, and source name.
 *                             An empty string means no filter is active.
 * @property isRefreshing      `true` while a background network refresh is in progress (e.g.,
 *                             category change, initial load); used to show the non-blocking
 *                             "Refreshing…" pill indicator above the list.
 * @property isPullRefreshing  `true` while a user-initiated pull-to-refresh is in progress.
 *                             Kept separate from [isRefreshing] so the pull indicator only
 *                             appears in response to the physical pull gesture, not during
 *                             automatic background refreshes.
 * @property errorMessage      A user-facing error description when the most recent network
 *                             refresh fails, or `null` when there is no error.
 */
data class HomeUiState(
    val selectedCategory: String = "general",
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val isPullRefreshing: Boolean = false,
    val errorMessage: String? = null
)

