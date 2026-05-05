package com.example.news.domain.usecase

import com.example.news.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFollowedCategoriesUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(): Flow<Set<String>> {
        return newsRepository.observeFollowedCategories()
    }
}
