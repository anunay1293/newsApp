package com.example.news.domain.usecase

import com.example.news.domain.repository.NewsRepository
import javax.inject.Inject

class ToggleFollowCategoryUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(categoryId: String) {
        newsRepository.toggleFollowCategory(categoryId)
    }
}
