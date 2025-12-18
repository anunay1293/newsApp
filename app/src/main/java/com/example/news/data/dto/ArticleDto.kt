package com.example.news.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for a single article from AWS API Gateway feed endpoint.
 */
data class ArticleDto(
    @SerializedName("title") val title: String?,
    @SerializedName("author") val author: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("sourceName") val sourceName: String?
)

