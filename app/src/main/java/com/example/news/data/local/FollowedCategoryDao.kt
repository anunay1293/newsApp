package com.example.news.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedCategory(entity: FollowedCategoryEntity)

    @Delete
    suspend fun deleteFollowedCategory(entity: FollowedCategoryEntity)

    @Query("SELECT * FROM followed_categories WHERE categoryId = :categoryId")
    suspend fun getFollowedCategory(categoryId: String): FollowedCategoryEntity?

    @Query("SELECT categoryId FROM followed_categories")
    suspend fun getAllFollowedCategoryIds(): List<String>

    @Query("SELECT categoryId FROM followed_categories")
    fun observeAllFollowedCategoryIds(): Flow<List<String>>

    @Query("DELETE FROM followed_categories")
    suspend fun deleteAllFollowedCategories()
}
