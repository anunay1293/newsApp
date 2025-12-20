package com.example.news.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for AWS API Gateway feed response.
 */
data class FeedResponseDto(
    @SerializedName("category") val category: String?,
    @SerializedName("articles") val articles: List<ArticleDto>?
)

