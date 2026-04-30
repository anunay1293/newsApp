package com.example.news.data.mapper

import com.example.news.data.dto.ArticleDto
import com.example.news.data.local.ArticleEntity
import com.example.news.domain.model.Article
import java.security.MessageDigest
import java.util.UUID

/**
 * Maps an [ArticleDto] (raw API response) to an [ArticleEntity] (Room table row).
 *
 * A stable, deterministic [ArticleEntity.articleId] is generated from the article's URL
 * via SHA-256 hashing, ensuring the same article always maps to the same primary key
 * (critical for upsert / deduplication logic in Room). If the URL is `null` a random UUID
 * is used as a fallback.
 *
 * Null/blank fields are replaced with sensible defaults so Room columns are never empty.
 *
 * @param category The news category tag to attach to this entity (e.g., "technology"),
 *                 used for category-based Room queries.
 * @return A fully populated [ArticleEntity] ready for database insertion.
 */
fun ArticleDto.toEntity(category: String): ArticleEntity {
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
 * Maps an [ArticleEntity] (Room table row) to a domain [Article].
 *
 * Parses the stored ISO 8601 [publishedAt] string into a Unix-epoch millisecond timestamp
 * and applies the same null/blank-handling defaults as the DTO mapper.
 *
 * @param isBookmarked Whether the current user has bookmarked this article. Defaults to
 *                     `false`; the repository sets this based on the bookmark cache.
 * @return A domain-layer [Article] instance.
 */
fun ArticleEntity.toDomain(isBookmarked: Boolean = false): Article {
    val publishedDate = parsePublishedDate(publishedAt)
    
    return Article(
        id = articleId,
        title = title,
        author = author.takeIf { it.isNotBlank() } ?: "Unknown",
        publishedDate = publishedDate,
        imageUrl = urlToImage?.takeIf { it.isNotBlank() },
        articleUrl = url,
        isBookmarked = isBookmarked,
        category = category
    )
}

/**
 * Generates a deterministic article identifier from the given [url] using SHA-256 hashing.
 *
 * The full 64-character hex digest is used as the ID, ensuring uniqueness and stability
 * across app sessions. If SHA-256 is unavailable (should never happen on Android), the
 * function falls back to [String.hashCode].
 *
 * @param url The article's canonical URL.
 * @return A hex-encoded SHA-256 hash string, or the hash-code string on failure.
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
 * Parses an ISO 8601 date string into a Unix-epoch millisecond timestamp.
 *
 * Supports multiple common variants (with/without milliseconds, 'Z' suffix vs. numeric
 * offset). Returns [System.currentTimeMillis] if the input is null, blank, or unparseable.
 *
 * @param dateString The ISO 8601 date string to parse, e.g., `"2025-02-23T14:30:00Z"`.
 * @return Parsed timestamp in milliseconds, or current time as a fallback.
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

