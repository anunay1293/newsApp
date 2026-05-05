package com.example.news.ui.articledetail

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import com.example.news.presentation.articledetail.ArticleSummaryState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.news.R
import com.example.news.presentation.articledetail.ArticleDetailScreenEvents
import com.example.news.presentation.articledetail.ArticleDetailViewModel
import com.example.news.ui.mapper.toUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    onNavigateBack: () -> Unit,
    onSignInRequired: (String) -> Unit = {},
    onFollowSignInRequired: (String) -> Unit = {},
    pendingBookmarkArticleId: String? = null,
    onPendingBookmarkConsumed: () -> Unit = {},
    pendingFollowCategoryId: String? = null,
    onPendingFollowCategoryConsumed: () -> Unit = {}
) {
    val viewModel: ArticleDetailViewModel = hiltViewModel()
    val events: ArticleDetailScreenEvents = viewModel
    val uiState by viewModel.uiState.collectAsState()
    val article = uiState.article?.toUiModel()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.authRequiredForBookmark.collect { articleId ->
            onSignInRequired(articleId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.authRequiredForFollowCategory.collect { categoryId ->
            onFollowSignInRequired(categoryId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.followSnackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(pendingBookmarkArticleId) {
        val id = pendingBookmarkArticleId ?: return@LaunchedEffect
        events.onBookmarkToggle()
        onPendingBookmarkConsumed()
    }

    LaunchedEffect(pendingFollowCategoryId) {
        val id = pendingFollowCategoryId ?: return@LaunchedEffect
        events.onFollowCategoryToggle()
        onPendingFollowCategoryConsumed()
    }

    Scaffold(
        topBar = {
            ArticleDetailTopBar(
                article = article,
                isCategoryFollowed = uiState.isCategoryFollowed,
                onNavigateBack = onNavigateBack,
                events = events
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    ArticleDetailLoadingSection()
                }

                uiState.errorMessage != null -> {
                    ArticleDetailErrorSection(
                        errorMessage = uiState.errorMessage
                            ?: stringResource(R.string.error_article_not_found)
                    )
                }

                article != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ArticleSummarySection(summaryState = uiState.summaryState)
                        ArticleWebView(
                            url = article.articleUrl,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    var pageLoadProgress by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            pageLoadProgress = newProgress
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        AnimatedVisibility(
            visible = pageLoadProgress in 1..99,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { pageLoadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
