package com.example.news.domain.usecase

import com.example.news.domain.model.Article
import com.example.news.domain.repository.NewsRepository
import javax.inject.Inject

/**
 * Retrieves a single article by its unique identifier from the local database.
 *
 * Used by the article detail screen to load the full article data (title, URL,
 * image, bookmark state) needed for in-app display via WebView.
 */
class GetArticleByIdUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(articleId: String): Article? {
        return newsRepository.getArticleById(articleId)
    }
}
