package com.example.news.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object representing a single article in the JSON response from the
 * AWS API Gateway `/feed` endpoint.
 *
 * All fields are nullable because the upstream news source may omit any of them.
 * Mappers in `com.example.news.data.mapper` handle null-safety and provide defaults.
 *
 * @property title       The headline of the article.
 * @property author      The name of the article's author.
 * @property publishedAt ISO 8601 formatted publication date (e.g., `"2025-02-23T14:00:00Z"`).
 * @property url         The canonical URL to the full article on the publisher's website.
 * @property urlToImage  URL to the article's hero / thumbnail image.
 * @property sourceName  The name of the publishing source (e.g., "BBC News", "CNN").
 */
data class ArticleDto(
    @SerializedName("title") val title: String?,
    @SerializedName("author") val author: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("sourceName") val sourceName: String?
)

