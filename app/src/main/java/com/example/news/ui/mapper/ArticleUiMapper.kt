package com.example.news.ui.mapper

import com.example.news.domain.model.Article
import com.example.news.ui.model.ArticleUiModel

/**
 * Maps a domain [Article] to a UI-layer [ArticleUiModel].
 *
 * This mapping lives in the UI layer so that the presentation layer (ViewModels, UiState)
 * depends only on domain models. Composables call this extension right before rendering,
 * keeping the layer boundary clean.
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
