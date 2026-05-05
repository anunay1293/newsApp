package com.example.news.presentation.home

interface HomeScreenEvents {
    fun onCategorySelected(category: String)
    fun onSearchQueryChanged(searchQuery: String)
    fun onBookmarkToggle(articleId: String, isCurrentlyBookmarked: Boolean)
    fun onRetryClicked()
    fun onPullToRefresh()
    fun onFollowCategoryToggle(categoryId: String, isCurrentlyFollowed: Boolean)

    companion object : HomeScreenEvents {
        override fun onCategorySelected(category: String) = Unit
        override fun onSearchQueryChanged(searchQuery: String) = Unit
        override fun onBookmarkToggle(articleId: String, isCurrentlyBookmarked: Boolean) = Unit
        override fun onRetryClicked() = Unit
        override fun onPullToRefresh() = Unit
        override fun onFollowCategoryToggle(categoryId: String, isCurrentlyFollowed: Boolean) = Unit
    }
}
