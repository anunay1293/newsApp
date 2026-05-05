package com.example.news.domain.usecase

import com.example.news.domain.repository.ArticleSummaryRepository
import javax.inject.Inject

class GetArticleSummaryUseCase @Inject constructor(
    private val repository: ArticleSummaryRepository
) {
    suspend operator fun invoke(articleId: String, articleUrl: String, title: String): Result<List<String>> =
        runCatching { repository.getArticleSummary(articleId, articleUrl, title) }
}
