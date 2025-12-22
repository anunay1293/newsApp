package com.example.news.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing bookmarked articles.
 * References ArticleEntity via articleId (foreign key).
 */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["articleId"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE // If article is deleted, bookmark is also deleted
        )
    ],
    indices = [Index(value = ["articleId"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey
    val articleId: String,
    val bookmarkedAt: Long = System.currentTimeMillis()
)

