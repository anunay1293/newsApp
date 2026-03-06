package com.example.news.domain.usecase

import com.example.news.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Retrieves the email address of the currently signed-in user.
 *
 * Used by the presentation layer to display a "Signed in as …" label without
 * directly depending on the auth provider SDK.
 */
class GetCurrentUserEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): String? = authRepository.getCurrentUserEmail()
}
