package com.example.news.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a cached news article stored in the local database.
 *
 * Each row corresponds to a single article fetched from the remote API. The table is
 * indexed on [category] to accelerate the most common query pattern (paginated articles
 * by category). The primary key [articleId] is a deterministic SHA-256 hash of the
 * article URL, ensuring stable deduplication across network refreshes.
 *
 * @property articleId  Stable unique identifier derived from the article URL (SHA-256 hex hash).
 * @property category   The news category this article belongs to (e.g., "technology").
 * @property title      The headline / title of the article.
 * @property author     The article author's name; defaults to "Unknown" if unavailable.
 * @property publishedAt ISO 8601 formatted publication date string from the API.
 * @property url        The full URL to the article on the publisher's website.
 * @property urlToImage Optional URL to the article's hero image; `null` when unavailable.
 * @property sourceName Optional name of the publishing source (e.g., "BBC News").
 * @property fetchedAt  Unix-epoch millisecond timestamp recording when this row was inserted
 *                      or last updated; used for staleness calculations during cleanup.
 */
@Entity(
    tableName = "articles",
    indices = [Index(value = ["category"])]
)
data class ArticleEntity(
    @PrimaryKey
    val articleId: String,
    val category: String,
    val title: String,
    val author: String,
    val publishedAt: String,
    val url: String,
    val urlToImage: String?,
    val sourceName: String?,
    val fetchedAt: Long
)

