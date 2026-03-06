package com.example.news.data.api

import com.example.news.data.dto.BookmarkDto
import com.example.news.data.dto.BookmarksResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BookmarkApiService {
    @GET("bookmarks")
    suspend fun getBookmarks(): BookmarksResponseDto

    @POST("bookmarks")
    suspend fun addBookmark(@Body bookmark: BookmarkDto)

    @DELETE("bookmarks/{articleId}")
    suspend fun removeBookmark(@Path("articleId") articleId: String)
}
