package com.example.news.data.dto

data class FollowedCategoryDto(
    val categoryId: String,
    val followedAt: Long
)

data class FollowedCategoriesResponseDto(
    val categories: List<FollowedCategoryDto>
)
