package com.example.chitchat.retrofit

import com.example.chitchat.config.InternetAddress
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = InternetAddress.retrofitAddress

    private val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}