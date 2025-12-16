package com.example.news.ui.home

import com.example.news.ui.model.ArticleUiModel

/**
 * Provides fake in-memory data for HomeScreen.
 * TODO: Replace with ViewModel that fetches data from repository.
 */
object FakeDataProvider {
    private val now = System.currentTimeMillis()
    private val oneDayMillis = 24 * 60 * 60 * 1000L

    fun getFakeArticles(): List<ArticleUiModel> = listOf(
        ArticleUiModel(
            id = "1",
            title = "Breaking: Major Technology Breakthrough Announced",
            author = "Jane Smith",
            publishedDate = now - (2 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=1",
            articleUrl = "https://example.com/article/1"
        ),
        ArticleUiModel(
            id = "2",
            title = "Global Markets React to New Economic Policies",
            author = "John Doe",
            publishedDate = now - (5 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=2",
            articleUrl = "https://example.com/article/2"
        ),
        ArticleUiModel(
            id = "3",
            title = "Championship Finals: Who Will Win?",
            author = null, // Testing "Unknown" author case
            publishedDate = now - (1 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=3",
            articleUrl = "https://example.com/article/3"
        ),
        ArticleUiModel(
            id = "4",
            title = "New Study Reveals Health Benefits of Daily Exercise",
            author = "Dr. Sarah Johnson",
            publishedDate = now - (3 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=4",
            articleUrl = "https://example.com/article/4"
        ),
        ArticleUiModel(
            id = "5",
            title = "Space Exploration: Mission to Mars Update",
            author = "Michael Chen",
            publishedDate = now - (7 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=5",
            articleUrl = "https://example.com/article/5"
        ),
        ArticleUiModel(
            id = "6",
            title = "Entertainment Industry Celebrates Annual Awards",
            author = "Emma Williams",
            publishedDate = now - (4 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=6",
            articleUrl = "https://example.com/article/6"
        ),
        ArticleUiModel(
            id = "7",
            title = "Tech Giants Announce Partnership",
            author = "Alex Brown",
            publishedDate = now - (6 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=7",
            articleUrl = "https://example.com/article/7"
        ),
        ArticleUiModel(
            id = "8",
            title = "Scientific Discovery Could Change Medicine",
            author = "Prof. Robert Lee",
            publishedDate = now - (8 * oneDayMillis),
            imageUrl = "https://picsum.photos/400/250?random=8",
            articleUrl = "https://example.com/article/8"
        )
    )
}

