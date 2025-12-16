package com.example.news.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for article source from NewsAPI.
 */
data class SourceDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

