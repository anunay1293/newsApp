package com.example.news.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database serving as the local persistence layer for news articles and bookmarks.
 *
 * Contains two tables:
 * - **articles** ([ArticleEntity]) – cached news articles fetched from the remote API.
 * - **bookmarks** ([BookmarkEntity]) – records of articles the user has bookmarked.
 *
 * The database is accessed via two DAOs: [ArticleDao] and [BookmarkDao].
 *
 * Schema versioning: currently at version 2 with destructive migration enabled for
 * development convenience. In production, replace [fallbackToDestructiveMigration]
 * with explicit [Migration] objects to preserve user data across upgrades.
 */
@Database(
    entities = [ArticleEntity::class, BookmarkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NewsDatabase : RoomDatabase() {

    /** Provides access to article CRUD and paging operations. */
    abstract fun articleDao(): ArticleDao

    /** Provides access to bookmark CRUD, observation, and paged queries. */
    abstract fun bookmarkDao(): BookmarkDao
    
    companion object {
        /** Double-checked locking singleton instance; marked [Volatile] for thread safety. */
        @Volatile
        private var INSTANCE: NewsDatabase? = null
        
        /**
         * Returns the singleton [NewsDatabase] instance, creating it on first access.
         *
         * Uses double-checked locking (`synchronized`) to ensure only one database
         * instance is ever created, even under concurrent access from multiple threads.
         *
         * @param context Any [Context]; [applicationContext] is used internally to
         *                prevent Activity-scoped memory leaks.
         * @return The singleton [NewsDatabase] instance.
         */
        fun getDatabase(context: Context): NewsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NewsDatabase::class.java,
                    "news_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

