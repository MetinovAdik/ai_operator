package com.myfreax.audiorecorder.openai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object OpenAIChatRetrofitClient {
    private const val BASE_URL = "https://api.openai.com/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .readTimeout(30, TimeUnit.SECONDS)  // Increase read timeout
        .connectTimeout(30, TimeUnit.SECONDS)  // Increase connection timeout
        .build()

    val apiService: OpenAIChatApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAIChatApiService::class.java)
}
