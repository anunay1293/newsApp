package com.example.news.data.mapper

import com.example.news.data.dto.ArticleDto
import com.example.news.ui.model.ArticleUiModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Legacy convenience mapper: [ArticleDto] → [ArticleUiModel] (bypasses Room).
 *
 * The current production flow is [ArticleDto.toEntity] → Room → [ArticleEntity.toDomain]
 * → [Article.toUiModel]. This function is retained for potential use in scenarios where
 * articles are displayed without local persistence (e.g., search-only results).
 *
 * @return A fully populated [ArticleUiModel] with sensible defaults for any missing fields.
 */
fun ArticleDto.toUiModel(): ArticleUiModel {
    val id = url?.hashCode()?.toString() ?: UUID.randomUUID().toString()
    val title = title ?: "No title available"
    val author = author?.takeIf { it.isNotBlank() } ?: "Unknown"
    val publishedDate = parsePublishedDate(publishedAt)
    val imageUrl = urlToImage?.takeIf { it.isNotBlank() }
    val articleUrl = url ?: ""
    
    return ArticleUiModel(
        id = id,
        title = title,
        author = author,
        publishedDate = publishedDate,
        imageUrl = imageUrl,
        articleUrl = articleUrl
    )
}

/**
 * Parses an ISO 8601 date string into a Unix-epoch millisecond timestamp.
 *
 * Attempts multiple common ISO 8601 variants in order:
 * 1. `yyyy-MM-dd'T'HH:mm:ss'Z'`
 * 2. `yyyy-MM-dd'T'HH:mm:ssZ`
 * 3. `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`
 * 4. `yyyy-MM-dd'T'HH:mm:ss.SSSZ`
 *
 * @param dateString The date string from the API, e.g., `"2025-01-15T10:30:00Z"`.
 *                   May be `null` or blank.
 * @return The parsed timestamp in milliseconds, or [System.currentTimeMillis] if the
 *         input is null, blank, or does not match any supported format.
 */
private fun parsePublishedDate(dateString: String?): Long {
    if (dateString == null || dateString.isBlank()) {
        return System.currentTimeMillis()
    }
    
    // Try multiple date formats
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    )
    
    for (formatPattern in formats) {
        try {
            val format = SimpleDateFormat(formatPattern, Locale.getDefault())
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return format.parse(dateString)?.time ?: continue
        } catch (e: Exception) {
            // Try next format
            continue
        }
    }
    
    // If all formats fail, return current time
    return System.currentTimeMillis()
}

