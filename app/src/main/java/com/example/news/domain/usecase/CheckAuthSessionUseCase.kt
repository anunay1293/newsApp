package com.example.news.domain.usecase

import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Checks whether the user has an active, valid authentication session.
 */
class CheckAuthSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.checkSession()
    }
}
