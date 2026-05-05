package com.example.news.di

import android.content.Context
import androidx.room.Room
import com.example.news.data.local.ArticleDao
import com.example.news.data.local.BookmarkDao
import com.example.news.data.local.FollowedCategoryDao
import com.example.news.data.local.NewsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and its DAOs as application-scoped singletons.
 *
 * Replaces the former `NewsDatabase.getDatabase()` companion-object singleton with
 * Dagger-managed lifecycle, enabling direct constructor injection of DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewsDatabase {
        return Room.databaseBuilder(
            context,
            NewsDatabase::class.java,
            "news_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideArticleDao(database: NewsDatabase): ArticleDao = database.articleDao()

    @Provides
    fun provideBookmarkDao(database: NewsDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideFollowedCategoryDao(database: NewsDatabase): FollowedCategoryDao =
        database.followedCategoryDao()
}
