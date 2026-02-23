package com.example.news.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton module that provides a pre-configured [NewsApiService] backed by Retrofit and OkHttp.
 *
 * Configuration highlights:
 * - **Base URL** points to the AWS API Gateway production stage (`/prod/`).
 * - **OkHttp** includes an [HttpLoggingInterceptor] at `BODY` level for development
 *   debugging. In release builds this should be reduced to `NONE` to avoid leaking
 *   response payloads to Logcat.
 * - **Timeouts** are set to 30 seconds for connect, read, and write to accommodate
 *   potentially slow Lambda cold starts behind API Gateway.
 * - **Gson** is used as the JSON converter for deserialising [FeedResponseDto] and
 *   [ArticleDto] objects.
 *
 * The API is public and does not require API keys or authorization headers.
 */
object NewsApiModule {

    /** Base URL for the AWS API Gateway news feed endpoint (production stage). */
    private const val BASE_URL = "https://eqzdxpsxvf.execute-api.us-east-1.amazonaws.com/prod/"
    
    /** HTTP request/response logging interceptor for development debugging. */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /** Configured OkHttp client with logging and generous timeouts. */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /** Retrofit instance targeting the API Gateway base URL with Gson deserialization. */
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    /** Singleton [NewsApiService] instance ready for use by repository classes. */
    val newsApiService: NewsApiService = retrofit.create(NewsApiService::class.java)
}

