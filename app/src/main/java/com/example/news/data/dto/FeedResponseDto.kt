package com.example.news.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object representing the top-level JSON response from the
 * AWS API Gateway `/feed` endpoint.
 *
 * The response wraps a list of [ArticleDto] objects under the `"articles"` key,
 * along with the echoed `"category"` that was requested.
 *
 * @property category The news category that was queried (e.g., "technology").
 *                    Echoed back from the request for client-side verification.
 * @property articles The list of article objects returned for this category;
 *                    may be `null` or empty if no articles are available.
 */
data class FeedResponseDto(
    @SerializedName("category") val category: String?,
    @SerializedName("articles") val articles: List<ArticleDto>?
)

