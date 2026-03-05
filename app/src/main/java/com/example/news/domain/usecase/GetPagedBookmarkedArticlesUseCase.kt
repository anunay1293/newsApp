package com.example.news.domain.usecase

import androidx.paging.PagingData
import com.example.news.domain.model.Article
import com.example.news.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves a paginated stream of all bookmarked articles, ordered by the time
 * they were bookmarked (most recent first).
 *
 * The flow re-emits whenever the bookmarks table changes, triggering a fresh
 * PagingSource for the consumer.
 */
class GetPagedBookmarkedArticlesUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(): Flow<PagingData<Article>> {
        return newsRepository.getPagedBookmarkedArticles()
    }
}
