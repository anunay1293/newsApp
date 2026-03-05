package com.example.news.presentation.mapper

import com.example.news.domain.model.Article
import com.example.news.ui.model.ArticleUiModel

/**
 * Maps a domain [Article] to a presentation-layer [ArticleUiModel].
 *
 * This mapping lives in the presentation layer because [ArticleUiModel] is a UI concern.
 * The domain layer remains unaware of how articles are rendered on screen.
 */
fun Article.toUiModel(): ArticleUiModel {
    return ArticleUiModel(
        id = id,
        title = title,
        author = author,
        publishedDate = publishedDate,
        imageUrl = imageUrl,
        articleUrl = articleUrl,
        isBookmarked = isBookmarked
    )
}
