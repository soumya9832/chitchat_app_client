package com.example.chitchat.model

import android.os.Parcelable
import com.example.chitchat.util.Status
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Int,
    val userName:String,
    val imageUrl:String,
    val status: Status
):Parcelable
