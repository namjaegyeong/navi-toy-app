package com.example.navigationtoyproject.api.di

import com.example.navigationtoyproject.api.request.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://172.17.0.131:8080/" // Replace with your API base URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Set to BODY for detailed logs
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Add logging interceptor
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Set the OkHttp client
            .addConverterFactory(GsonConverterFactory.create()) // Add Gson converter
            .build()
            .create(ApiService::class.java)
    }
}