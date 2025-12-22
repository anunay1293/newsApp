package com.example.news.ui.bookmarks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import android.app.Application
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.news.presentation.bookmarks.BookmarksUiEvent
import com.example.news.presentation.bookmarks.BookmarksViewModel
import com.example.news.ui.model.ArticleUiModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying all bookmarked articles.
 */
@Composable
fun BookmarksScreen() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: BookmarksViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return BookmarksViewModel(application) as T
            }
        }
    )
    val pagedArticles = viewModel.pagedArticles.collectAsLazyPagingItems()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your saved articles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Article list
        Box(modifier = Modifier.weight(1f)) {
            if (pagedArticles.loadState.refresh is LoadState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading bookmarks...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (pagedArticles.itemCount == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(48.dp)
                    ) {
                        Text(
                            text = "No bookmarks yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bookmark articles to read them later",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        count = pagedArticles.itemCount,
                        key = pagedArticles.itemKey { it.id }
                    ) { index ->
                        val article = pagedArticles[index]
                        if (article != null) {
                            ArticleCard(
                                article = article,
                                onBookmarkToggle = { articleId ->
                                    viewModel.handleEvent(BookmarksUiEvent.OnBookmarkToggle(articleId))
                                }
                            )
                        }
                    }
                    
                    if (pagedArticles.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Article card component (reused from HomeScreen but with bookmark toggle).
 */
@Composable
private fun ArticleCard(
    article: ArticleUiModel,
    onBookmarkToggle: (String) -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.articleUrl))
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            article.imageUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = article.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }
            }
            
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.titleLarge.lineHeight
                    )
                    IconButton(
                        onClick = { onBookmarkToggle(article.id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (article.isBookmarked) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (article.isBookmarked) {
                                "Remove bookmark"
                            } else {
                                "Add bookmark"
                            },
                            tint = if (article.isBookmarked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = article.author ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = formatDate(article.publishedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

