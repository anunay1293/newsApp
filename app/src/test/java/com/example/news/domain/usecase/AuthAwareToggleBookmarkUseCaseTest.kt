package com.example.news.domain.usecase

import com.example.news.domain.model.BookmarkToggleResult
import com.example.news.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthAwareToggleBookmarkUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk(relaxUnitFun = true)

    private lateinit var useCase: AuthAwareToggleBookmarkUseCase

    @Before
    fun setUp() {
        useCase = AuthAwareToggleBookmarkUseCase(authRepository, toggleBookmarkUseCase)
    }

    @Test
    fun `given user is signed in, when bookmarking an article, then toggle succeeds`() = runTest {
        coEvery { authRepository.checkSession() } returns true

        val result = useCase("article-1", isCurrentlyBookmarked = false)

        assertTrue(result is BookmarkToggleResult.Success)
        coVerify { toggleBookmarkUseCase("article-1") }
    }

    @Test
    fun `given user is signed out, when bookmarking an article, then auth is required`() = runTest {
        coEvery { authRepository.checkSession() } returns false

        val result = useCase("article-1", isCurrentlyBookmarked = false)

        assertTrue(result is BookmarkToggleResult.AuthRequired)
        assertEquals("article-1", (result as BookmarkToggleResult.AuthRequired).articleId)
        coVerify(exactly = 0) { toggleBookmarkUseCase(any()) }
    }

    @Test
    fun `given user is signed out, when un-bookmarking an article, then toggle succeeds`() = runTest {
        coEvery { authRepository.checkSession() } returns false

        val result = useCase("article-1", isCurrentlyBookmarked = true)

        assertTrue(result is BookmarkToggleResult.Success)
        coVerify { toggleBookmarkUseCase("article-1") }
    }

    @Test
    fun `given user is signed in, when un-bookmarking an article, then toggle succeeds`() = runTest {
        coEvery { authRepository.checkSession() } returns true

        val result = useCase("article-1", isCurrentlyBookmarked = true)

        assertTrue(result is BookmarkToggleResult.Success)
        coVerify { toggleBookmarkUseCase("article-1") }
    }
}
