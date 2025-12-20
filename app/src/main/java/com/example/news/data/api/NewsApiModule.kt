package com.example.news.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Provides Retrofit instance and NewsApiService.
 * Sets up OkHttp with logging (debug only).
 * AWS API Gateway endpoint is public and doesn't require API key.
 */
object NewsApiModule {
    private const val BASE_URL = "https://eqzdxpsxvf.execute-api.us-east-1.amazonaws.com/prod/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Enable logging for debug builds
        // In release builds, you can set this to Level.NONE if needed
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val newsApiService: NewsApiService = retrofit.create(NewsApiService::class.java)
}

