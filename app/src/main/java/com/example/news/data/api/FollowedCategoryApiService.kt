package com.example.news.data.api

import com.example.news.data.dto.FollowedCategoriesResponseDto
import com.example.news.data.dto.FollowedCategoryDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FollowedCategoryApiService {

    @GET("followed-categories")
    suspend fun getFollowedCategories(): FollowedCategoriesResponseDto

    @POST("followed-categories")
    suspend fun addFollowedCategory(@Body dto: FollowedCategoryDto)

    @DELETE("followed-categories/{categoryId}")
    suspend fun removeFollowedCategory(@Path("categoryId") categoryId: String)
}
