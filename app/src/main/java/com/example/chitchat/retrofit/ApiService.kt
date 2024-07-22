package com.example.chitchat.retrofit

import com.example.chitchat.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST(value = "users/post")
    fun addUser(@Body user: User): Call<User>
}