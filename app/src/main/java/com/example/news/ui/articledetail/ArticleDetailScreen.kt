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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.news.R
import com.example.news.presentation.articledetail.ArticleDetailUiEvent
import com.example.news.presentation.articledetail.ArticleDetailViewModel
import com.example.news.ui.mapper.toUiModel

/**
 * Full-screen article detail screen that loads the article's web page in an embedded WebView.
 *
 * The screen consists of:
 * - A [TopAppBar] with a back-navigation arrow, the article title (single-line, truncated),
 *   and a bookmark toggle icon (filled/outlined heart).
 * - An [AndroidView]-hosted [WebView] that fills the remaining space, rendering the article
 *   URL so the user can read the full content without leaving the app.
 * - A [LinearProgressIndicator] that reflects the WebView's page-load progress.
 *
 * When the article data is still loading from Room, a centered [CircularProgressIndicator]
 * is shown. If the article cannot be found, an error message is displayed instead.
 *
 * @param onNavigateBack Callback invoked when the user taps the back arrow in the TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: ArticleDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val article = uiState.article?.toUiModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = article?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    if (article != null) {
                        IconButton(
                            onClick = {
                                viewModel.handleEvent(ArticleDetailUiEvent.OnBookmarkToggle)
                            }
                        ) {
                            Icon(
                                imageVector = if (article.isBookmarked) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                contentDescription = if (article.isBookmarked) {
                                    stringResource(R.string.cd_remove_bookmark)
                                } else {
                                    stringResource(R.string.cd_add_bookmark)
                                },
                                tint = if (article.isBookmarked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.error_article_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(innerPadding)
                    )
                }

                article != null -> {
                    ArticleWebView(
                        url = article.articleUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Composable wrapper around an Android [WebView] that loads the given [url].
 *
 * Displays a [LinearProgressIndicator] at the top that tracks page-load progress via
 * [WebChromeClient.onProgressChanged]. JavaScript is enabled so most modern article
 * pages render correctly.
 *
 * @param url      The article URL to load in the WebView.
 * @param modifier Modifier applied to the outer container.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    var pageLoadProgress by remember { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = pageLoadProgress in 1..99,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { pageLoadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
