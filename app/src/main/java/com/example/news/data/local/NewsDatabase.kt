package com.example.news.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database serving as the local persistence layer for news articles and bookmarks.
 *
 * Contains two tables:
 * - **articles** ([ArticleEntity]) -- cached news articles fetched from the remote API.
 * - **bookmarks** ([BookmarkEntity]) -- records of articles the user has bookmarked.
 *
 * The database is accessed via two DAOs: [ArticleDao] and [BookmarkDao].
 * The singleton lifecycle is managed by Hilt via [DatabaseModule].
 *
 * Schema versioning: currently at version 2 with destructive migration enabled for
 * development convenience. In production, replace destructive migration with explicit
 * [Migration] objects to preserve user data across upgrades.
 */
@Database(
    entities = [ArticleEntity::class, BookmarkEntity::class, FollowedCategoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class NewsDatabase : RoomDatabase() {

    /** Provides access to article CRUD and paging operations. */
    abstract fun articleDao(): ArticleDao

    /** Provides access to bookmark CRUD, observation, and paged queries. */
    abstract fun bookmarkDao(): BookmarkDao

    /** Provides access to followed-category CRUD and observation. */
    abstract fun followedCategoryDao(): FollowedCategoryDao
}

