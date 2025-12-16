package com.example.news.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for NewsAPI top headlines response.
 */
data class TopHeadlinesResponseDto(
    @SerializedName("status") val status: String?,
    @SerializedName("totalResults") val totalResults: Int?,
    @SerializedName("articles") val articles: List<ArticleDto>?
)

