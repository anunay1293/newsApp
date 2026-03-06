package com.example.news.presentation.bookmarks

interface BookmarksScreenEvents {
    fun onBookmarkToggle(articleId: String)
    fun onRecheckAuth()

    companion object : BookmarksScreenEvents {
        override fun onBookmarkToggle(articleId: String) = Unit
        override fun onRecheckAuth() = Unit
    }
}
