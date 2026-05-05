package com.example.news.presentation.articledetail

interface ArticleDetailScreenEvents {
    fun onBookmarkToggle()
    fun onFollowCategoryToggle()

    companion object : ArticleDetailScreenEvents {
        override fun onBookmarkToggle() = Unit
        override fun onFollowCategoryToggle() = Unit
    }
}
