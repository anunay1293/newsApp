package com.example.news.presentation.articledetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.news.domain.model.Article
import com.example.news.domain.model.BookmarkToggleResult
import com.example.news.domain.usecase.AuthAwareToggleBookmarkUseCase
import com.example.news.domain.usecase.GetArticleByIdUseCase
import com.example.news.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getArticleByIdUseCase: GetArticleByIdUseCase = mockk()
    private val authAwareToggleBookmarkUseCase: AuthAwareToggleBookmarkUseCase = mockk()

    private val testArticle = Article(
        id = "art1",
        title = "Test Article",
        author = "Author",
        publishedDate = 1_000_000L,
        imageUrl = null,
        articleUrl = "https://example.com",
        isBookmarked = false
    )

    private fun createViewModel(): ArticleDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("articleId" to "art1"))
        return ArticleDetailViewModel(
            savedStateHandle,
            getArticleByIdUseCase,
            authAwareToggleBookmarkUseCase
        )
    }

    @Test
    fun `given article exists in DB, when VM initializes, then article is loaded`() = runTest {
        coEvery { getArticleByIdUseCase("art1") } returns testArticle

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(testArticle, state.article)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `given article was pruned from cache, when VM initializes, then not found error shown`() = runTest {
        coEvery { getArticleByIdUseCase("art1") } returns null

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.article)
        assertFalse(state.isLoading)
        assertEquals("Article not found", state.errorMessage)
    }

    @Test
    fun `given DB error occurs, when VM initializes, then error message shown`() = runTest {
        coEvery { getArticleByIdUseCase("art1") } throws RuntimeException("DB read failed")

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.article)
        assertFalse(state.isLoading)
        assertEquals("DB read failed", state.errorMessage)
    }

    @Test
    fun `given article is loaded and not bookmarked and user is signed in, when toggling bookmark, then bookmark flips to true`() = runTest {
        coEvery { getArticleByIdUseCase("art1") } returns testArticle
        coEvery { authAwareToggleBookmarkUseCase("art1", false) } returns BookmarkToggleResult.Success

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onBookmarkToggle()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.article!!.isBookmarked)
    }

    @Test
    fun `given article is loaded and not bookmarked and user is signed out, when toggling bookmark, then auth required event emitted`() = runTest {
        coEvery { getArticleByIdUseCase("art1") } returns testArticle
        coEvery { authAwareToggleBookmarkUseCase("art1", false) } returns BookmarkToggleResult.AuthRequired("art1")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.authRequiredForBookmark.test {
            vm.onBookmarkToggle()
            advanceUntilIdle()

            assertEquals("art1", awaitItem())
        }

        assertFalse(vm.uiState.value.article!!.isBookmarked)
    }

    @Test
    fun `given article is bookmarked, when toggling bookmark, then bookmark flips to false`() = runTest {
        val bookmarkedArticle = testArticle.copy(isBookmarked = true)
        coEvery { getArticleByIdUseCase("art1") } returns bookmarkedArticle
        coEvery { authAwareToggleBookmarkUseCase("art1", true) } returns BookmarkToggleResult.Success

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onBookmarkToggle()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.article!!.isBookmarked)
    }

    @Test
    fun `given article failed to load, when toggling bookmark, then nothing happens`() = runTest {
        coEvery { getArticleByIdUseCase("art1") } returns null

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onBookmarkToggle()
        advanceUntilIdle()

        assertNull(vm.uiState.value.article)
    }
}
