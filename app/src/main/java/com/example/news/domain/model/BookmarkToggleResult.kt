package com.example.news.domain.model

sealed class BookmarkToggleResult {
    object Success : BookmarkToggleResult()
    data class AuthRequired(val articleId: String) : BookmarkToggleResult()
}
