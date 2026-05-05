package com.example.news.domain.model

sealed class CategoryFollowResult {
    object Success : CategoryFollowResult()
    data class AuthRequired(val categoryId: String) : CategoryFollowResult()
}
