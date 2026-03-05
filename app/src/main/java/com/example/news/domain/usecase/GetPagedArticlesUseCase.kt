package com.example.news.domain.usecase

import androidx.paging.PagingData
import com.example.news.domain.model.Article
import com.example.news.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves a paginated stream of articles for a given category and optional search query.
 *
 * Data is sourced from the local Room database (single source of truth) and updates
 * reactively when the underlying table changes.
 */
class GetPagedArticlesUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(category: String, searchQuery: String = ""): Flow<PagingData<Article>> {
        return newsRepository.getPagedArticles(category, searchQuery)
    }
}
