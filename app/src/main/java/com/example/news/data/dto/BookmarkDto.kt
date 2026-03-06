package com.example.news.data.dto

data class BookmarkDto(
    val articleId: String,
    val bookmarkedAt: Long,
    val title: String,
    val author: String,
    val publishedAt: String,
    val url: String,
    val urlToImage: String?,
    val sourceName: String?,
    val category: String
)

data class BookmarksResponseDto(
    val bookmarks: List<BookmarkDto>
)
