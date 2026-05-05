package com.example.news.ui.articledetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.news.R
import com.example.news.presentation.articledetail.ArticleSummaryState
import com.example.news.ui.theme.NewsTheme

@Composable
fun ArticleSummarySection(
    summaryState: ArticleSummaryState,
    modifier: Modifier = Modifier
) {
    if (summaryState == ArticleSummaryState.Idle) return

    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_auto_awesome),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse summary" else "Expand summary",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (isExpanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                ) {
                    when (summaryState) {
                        is ArticleSummaryState.Loading -> {
                            SummaryLoadingPlaceholder()
                        }
                        is ArticleSummaryState.Success -> {
                            summaryState.points.forEach { point ->
                                SummaryBulletPoint(text = point)
                            }
                        }
                        is ArticleSummaryState.Error -> {
                            Text(
                                text = "Summary unavailable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryLoadingPlaceholder() {
    repeat(3) {
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(vertical = 2.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleSummarySectionSuccessPreview() {
    NewsTheme {
        ArticleSummarySection(
            summaryState = ArticleSummaryState.Success(
                points = listOf(
                    "Roger from Acme Inc. provides an update on the \"Green Bot\" project, noting good progress on refining design concepts.",
                    "Ann needs to review mood boards and provide initial feedback on the visual directions presented.",
                    "The feedback will guide the team in developing the first detailed mockups for the project."
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleSummarySectionLoadingPreview() {
    NewsTheme {
        ArticleSummarySection(summaryState = ArticleSummaryState.Loading)
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleSummarySectionErrorPreview() {
    NewsTheme {
        ArticleSummarySection(summaryState = ArticleSummaryState.Error)
    }
}
