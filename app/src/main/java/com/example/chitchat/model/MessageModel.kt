package com.example.chitchat.model

data class MessageModel( val type: TYPE,
                         val name: String? = null,
                         val target: String? = null,
                         val data:Any?=null
)
