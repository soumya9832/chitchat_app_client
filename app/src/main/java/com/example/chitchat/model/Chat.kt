package com.example.chitchat.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class Chat(
    val chatId:String?=null,
    val senderId:String,
    val recipientId: String,
    val content: String
)
