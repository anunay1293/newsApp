package com.example.news.domain.usecase

import com.example.news.domain.model.AuthResult
import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Requests the auth provider to resend the sign-up confirmation code.
 */
class ResendCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): AuthResult {
        return authRepository.resendCode(email)
    }
}
