package com.example.news.presentation.articledetail

interface ArticleDetailScreenEvents {
    fun onBookmarkToggle()

    companion object : ArticleDetailScreenEvents {
        override fun onBookmarkToggle() = Unit
    }
}
