package com.example.news.domain.usecase

import com.example.news.domain.model.AuthResult
import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Authenticates an existing user with the given credentials.
 *
 * Clears any stale session before attempting sign-in to avoid conflicts.
 */
class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        return authRepository.signIn(email, password)
    }
}
