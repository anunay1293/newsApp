package com.example.news.domain.model

/**
 * Domain model representing a news article.
 *
 * This is the core business object used throughout the domain and presentation layers.
 * It is decoupled from both the database entity ([ArticleEntity]) and the network DTO
 * ([ArticleDto]), providing a stable contract that the rest of the app depends on.
 *
 * @property id            Stable unique identifier derived from the article URL (SHA-256 hash).
 * @property title         The headline of the article.
 * @property author        The article author's name.
 * @property publishedDate Publication timestamp as Unix epoch milliseconds.
 * @property imageUrl      Optional URL to the article's hero image; `null` when unavailable.
 * @property articleUrl    The full URL to the article on the publisher's website.
 * @property isBookmarked  Whether the current user has bookmarked this article.
 */
data class Article(
    val id: String,
    val title: String,
    val author: String,
    val publishedDate: Long,
    val imageUrl: String?,
    val articleUrl: String,
    val isBookmarked: Boolean = false,
    val category: String = ""
)
