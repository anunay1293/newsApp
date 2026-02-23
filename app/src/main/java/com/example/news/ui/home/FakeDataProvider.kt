package com.example.news.ui.home

import com.example.news.ui.model.ArticleUiModel

/**
 * Singleton that supplies hardcoded sample articles for use in Compose previews and
 * early-stage development before the real API integration was available.
 *
 * **Note:** This provider is no longer used at runtime — the app now fetches live data
 * through [NewsRepository][com.example.news.data.repository.NewsRepository]. It is retained
 * for preview composables and potential UI-testing scenarios.
 */
object FakeDataProvider {
    /** Reference timestamp (current wall-clock time) used to compute relative dates. */
    private val now = System.currentTimeMillis()

    /** Number of milliseconds in one calendar day. */
    private val oneDayMillis = 24 * 60 * 60 * 1000L

    /**
     * Returns a list of eight mock [ArticleUiModel] instances spanning various categories
     * and authors, with publication dates spread across the past week.
     *
     * @return An immutable list of sample articles suitable for preview rendering.
     */
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

