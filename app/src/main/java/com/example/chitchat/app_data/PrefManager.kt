package com.example.chitchat.app_data

import android.content.Context
import android.content.SharedPreferences
import com.example.chitchat.model.User
import com.google.gson.Gson

class PrefManager(context: Context) {
    private val prefName = "my_app_prefs"
    private val keyUser = "user"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    private val gson = Gson()


    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        editor.putString(keyUser, userJson)
        editor.apply()
    }

    fun getUser(): User? {
        val userJson = sharedPreferences.getString(keyUser, null)
        return userJson?.let {
            gson.fromJson(it, User::class.java)
        }
    }

    fun isUserSet(): Boolean {
        return sharedPreferences.contains(keyUser)
    }
    
}