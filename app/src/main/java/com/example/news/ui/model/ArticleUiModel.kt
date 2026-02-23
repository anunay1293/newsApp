package com.example.news.ui.model

/**
 * Presentation-layer model representing a single news article as displayed in the UI.
 *
 * Instances are produced by mapping [ArticleEntity][com.example.news.data.local.ArticleEntity]
 * records through [ArticleEntityMapper][com.example.news.data.mapper.toUiModel]. This model
 * is consumed by composables such as `ArticleCard` on both the home and bookmarks screens.
 *
 * @property id            Stable, unique identifier derived from the article URL (SHA-256 hash).
 * @property title         The headline / title of the article.
 * @property author        The article author's name, or `null` if the source didn't provide one.
 * @property publishedDate Publication timestamp as Unix epoch milliseconds; used by [formatDate]
 *                         to render a human-readable date string.
 * @property imageUrl      Optional URL to the article's hero / thumbnail image; `null` when
 *                         no image is available (the card hides the image section in that case).
 * @property articleUrl    The full URL opened in the browser when the user taps the card.
 * @property isBookmarked  Whether the current user has bookmarked this article; drives the
 *                         filled vs. outlined heart icon on the card.
 */
data class ArticleUiModel(
    val id: String,
    val title: String,
    val author: String?,
    val publishedDate: Long,
    val imageUrl: String?,
    val articleUrl: String,
    val isBookmarked: Boolean = false
)

