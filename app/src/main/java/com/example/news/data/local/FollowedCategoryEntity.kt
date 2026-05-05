package com.example.news.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_categories")
data class FollowedCategoryEntity(
    @PrimaryKey val categoryId: String,
    val followedAt: Long = System.currentTimeMillis()
)
