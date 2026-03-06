package com.example.news.domain.usecase

import com.example.news.domain.repository.NewsRepository
import javax.inject.Inject

/**
 * Fetches the latest articles for a category from the remote API and upserts them
 * into the local Room database.
 *
 * On success, Room automatically notifies PagingData observers. On failure the error
 * is silently swallowed so cached data remains available.
 */
class RefreshArticlesUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(category: String) {
        newsRepository.refreshArticles(category)
    }
}
