package com.example.news.domain.usecase

import com.example.news.domain.repository.NewsRepository
import javax.inject.Inject

/**
 * Toggles the bookmark state for a given article.
 *
 * If the article is currently bookmarked it will be un-bookmarked, and vice versa.
 * The underlying PagingSource is invalidated so both the feed and bookmarks screens
 * reflect the change automatically.
 */
class ToggleBookmarkUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(articleId: String) {
        newsRepository.toggleBookmark(articleId)
    }
}
