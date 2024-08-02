package com.example.chitchat.websocket.videosocketclient

import com.example.chitchat.model.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}