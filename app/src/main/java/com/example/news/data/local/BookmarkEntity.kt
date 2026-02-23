package com.example.news.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a user's bookmark for a specific article.
 *
 * Each row links to an [ArticleEntity] via the [articleId] foreign key. A cascading
 * delete rule ensures that if an article is removed from the `articles` table, its
 * corresponding bookmark row is automatically deleted as well.
 *
 * The `articleId` column has a unique index, preventing duplicate bookmarks for the
 * same article.
 *
 * @property articleId    The ID of the bookmarked article; matches [ArticleEntity.articleId].
 * @property bookmarkedAt Unix-epoch millisecond timestamp of when the bookmark was created.
 *                        Defaults to the current time at insertion. Used to sort the
 *                        bookmarks screen by most-recently-bookmarked first.
 */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["articleId"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["articleId"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey
    val articleId: String,
    val bookmarkedAt: Long = System.currentTimeMillis()
)

