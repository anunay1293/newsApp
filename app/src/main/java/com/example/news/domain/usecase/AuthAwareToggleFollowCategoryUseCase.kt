package com.example.news.domain.usecase

import com.example.news.domain.model.CategoryFollowResult
import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

class AuthAwareToggleFollowCategoryUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val toggleFollowCategoryUseCase: ToggleFollowCategoryUseCase
) {
    suspend operator fun invoke(
        categoryId: String,
        isCurrentlyFollowed: Boolean
    ): CategoryFollowResult {
        if (!isCurrentlyFollowed && !authRepository.checkSession()) {
            return CategoryFollowResult.AuthRequired(categoryId)
        }
        toggleFollowCategoryUseCase(categoryId)
        return CategoryFollowResult.Success
    }
}
