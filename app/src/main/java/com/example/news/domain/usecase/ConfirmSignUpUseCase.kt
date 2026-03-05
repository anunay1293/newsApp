package com.example.news.domain.usecase

import com.example.news.domain.model.AuthResult
import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Confirms a newly registered account by submitting the email verification code.
 */
class ConfirmSignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String): AuthResult {
        return authRepository.confirmSignUp(email, code)
    }
}
