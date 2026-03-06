package com.example.news.domain.usecase

import com.example.news.domain.model.BookmarkToggleResult
import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

class AuthAwareToggleBookmarkUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase
) {
    suspend operator fun invoke(articleId: String, isCurrentlyBookmarked: Boolean): BookmarkToggleResult {
        if (!isCurrentlyBookmarked && !authRepository.checkSession()) {
            return BookmarkToggleResult.AuthRequired(articleId)
        }
        toggleBookmarkUseCase(articleId)
        return BookmarkToggleResult.Success
    }
}
