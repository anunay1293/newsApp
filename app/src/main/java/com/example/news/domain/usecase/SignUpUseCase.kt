package com.example.news.domain.usecase

import com.example.news.domain.model.AuthResult
import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Registers a new user account with the given credentials.
 *
 * Typically returns [AuthResult.NeedsConfirmation] when email verification is required.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        return authRepository.signUp(email, password)
    }
}
