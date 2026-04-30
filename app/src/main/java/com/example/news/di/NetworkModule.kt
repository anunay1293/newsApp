package com.example.news.di

import com.example.news.data.api.BookmarkApiService
import com.example.news.data.api.CognitoAuthInterceptor
import com.example.news.data.api.FollowedCategoryApiService
import com.example.news.data.api.NewsApiService
import com.example.news.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authenticated

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://eqzdxpsxvf.execute-api.us-east-1.amazonaws.com/prod/"

    private const val BOOKMARK_API_BASE_URL = "https://ehztnmpmg4.execute-api.us-east-1.amazonaws.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun provideNewsApiService(retrofit: Retrofit): NewsApiService {
        return retrofit.create(NewsApiService::class.java)
    }

    @Provides
    @Singleton
    @Authenticated
    fun provideAuthenticatedOkHttpClient(
        okHttpClient: OkHttpClient,
        authRepository: AuthRepository
    ): OkHttpClient {
        return okHttpClient.newBuilder()
            .addInterceptor(CognitoAuthInterceptor(authRepository))
            .build()
    }

    @Provides
    @Singleton
    @Authenticated
    fun provideAuthenticatedRetrofit(
        @Authenticated okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BOOKMARK_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBookmarkApiService(@Authenticated retrofit: Retrofit): BookmarkApiService {
        return retrofit.create(BookmarkApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFollowedCategoryApiService(@Authenticated retrofit: Retrofit): FollowedCategoryApiService {
        return retrofit.create(FollowedCategoryApiService::class.java)
    }
}
