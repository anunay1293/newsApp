package com.example.news.data.mapper

import com.example.news.data.dto.ArticleDto
import com.example.news.ui.model.ArticleUiModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Maps ArticleDto to ArticleUiModel.
 * Handles missing/null fields gracefully with defaults.
 */
fun ArticleDto.toUiModel(): ArticleUiModel {
    // Generate ID from URL or use UUID if URL is null
    val id = url?.hashCode()?.toString() ?: UUID.randomUUID().toString()
    
    // Extract title or use default
    val title = title ?: "No title available"
    
    // Extract author or use "Unknown"
    val author = author?.takeIf { it.isNotBlank() } ?: "Unknown"
    
    // Parse published date (ISO 8601 format from API)
    val publishedDate = parsePublishedDate(publishedAt)
    
    // Extract image URL (nullable)
    val imageUrl = urlToImage?.takeIf { it.isNotBlank() }
    
    // Extract article URL or use empty string (shouldn't happen but handle gracefully)
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
 * Parses ISO 8601 date string to Unix timestamp in milliseconds.
 * Returns current time if parsing fails.
 * Handles formats like: "2024-01-15T10:30:00Z" or "2024-01-15T10:30:00+00:00"
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

