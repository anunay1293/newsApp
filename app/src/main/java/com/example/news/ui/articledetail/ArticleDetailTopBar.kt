package com.example.news.ui.articledetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.news.R
import com.example.news.presentation.articledetail.ArticleDetailScreenEvents
import com.example.news.ui.model.ArticleUiModel
import com.example.news.ui.theme.NewsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailTopBar(
    article: ArticleUiModel?,
    onNavigateBack: () -> Unit,
    events: ArticleDetailScreenEvents
) {
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
                    onClick = { events.onBookmarkToggle() }
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
}

@Preview
@Composable
private fun ArticleDetailTopBarPreview() {
    NewsTheme {
        ArticleDetailTopBar(
            article = ArticleUiModel(
                id = "1",
                title = "Breaking: Kotlin 2.0 Released",
                author = "Jane Doe",
                publishedDate = System.currentTimeMillis(),
                imageUrl = null,
                articleUrl = "https://example.com",
                isBookmarked = true
            ),
            onNavigateBack = {},
            events = ArticleDetailScreenEvents
        )
    }
}

@Preview
@Composable
private fun ArticleDetailTopBarNotBookmarkedPreview() {
    NewsTheme {
        ArticleDetailTopBar(
            article = ArticleUiModel(
                id = "2",
                title = "Android Compose Best Practices",
                author = "John Smith",
                publishedDate = System.currentTimeMillis(),
                imageUrl = null,
                articleUrl = "https://example.com",
                isBookmarked = false
            ),
            onNavigateBack = {},
            events = ArticleDetailScreenEvents
        )
    }
}
