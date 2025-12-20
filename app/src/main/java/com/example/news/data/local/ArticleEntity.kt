package com.example.news.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing articles locally.
 * Primary key is articleId (stable ID from URL).
 * Indexed on category for efficient queries.
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
    val fetchedAt: Long // Timestamp when article was stored locally
)

