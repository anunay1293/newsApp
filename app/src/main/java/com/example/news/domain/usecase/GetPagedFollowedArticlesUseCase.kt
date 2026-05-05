package com.example.news.domain.usecase

import androidx.paging.PagingData
import com.example.news.domain.model.Article
import com.example.news.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPagedFollowedArticlesUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(searchQuery: String = ""): Flow<PagingData<Article>> {
        return newsRepository.getPagedFollowedArticles(searchQuery)
    }
}
