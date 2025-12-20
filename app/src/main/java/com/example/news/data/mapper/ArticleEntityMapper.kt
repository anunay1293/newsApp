package com.example.news.data.mapper

import com.example.news.data.dto.ArticleDto
import com.example.news.data.local.ArticleEntity
import com.example.news.ui.model.ArticleUiModel
import java.security.MessageDigest
import java.util.UUID

/**
 * Maps ArticleDto (from API) to ArticleEntity (for Room storage).
 */
fun ArticleDto.toEntity(category: String): ArticleEntity {
    // Generate stable articleId from URL using SHA-256 hash
    val articleId = url?.let { generateArticleId(it) } ?: UUID.randomUUID().toString()
    
    return ArticleEntity(
        articleId = articleId,
        category = category,
        title = title ?: "No title available",
        author = author?.takeIf { it.isNotBlank() } ?: "Unknown",
        publishedAt = publishedAt ?: "",
        url = url ?: "",
        urlToImage = urlToImage?.takeIf { it.isNotBlank() },
        sourceName = sourceName?.takeIf { it.isNotBlank() },
        fetchedAt = System.currentTimeMillis()
    )
}

/**
 * Maps ArticleEntity (from Room) to ArticleUiModel (for UI).
 */
fun ArticleEntity.toUiModel(): ArticleUiModel {
    // Parse published date (ISO 8601 format)
    val publishedDate = parsePublishedDate(publishedAt)
    
    return ArticleUiModel(
        id = articleId,
        title = title,
        author = author.takeIf { it.isNotBlank() } ?: "Unknown",
        publishedDate = publishedDate,
        imageUrl = urlToImage?.takeIf { it.isNotBlank() },
        articleUrl = url
    )
}

/**
 * Generates a stable article ID from URL using SHA-256 hash.
 */
private fun generateArticleId(url: String): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(url.toByteArray())
        hash.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        // Fallback to URL hash code if SHA-256 fails
        url.hashCode().toString()
    }
}

/**
 * Parses ISO 8601 date string to Unix timestamp in milliseconds.
 * Returns current time if parsing fails.
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
            val format = java.text.SimpleDateFormat(formatPattern, java.util.Locale.getDefault())
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

