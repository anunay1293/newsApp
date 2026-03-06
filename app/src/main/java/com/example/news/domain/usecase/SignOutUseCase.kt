package com.example.news.domain.usecase

import com.example.news.domain.model.AuthResult
import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Signs the current user out of their authentication session.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AuthResult {
        return authRepository.signOut()
    }
}
