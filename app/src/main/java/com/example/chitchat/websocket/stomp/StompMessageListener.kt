package com.example.chitchat.websocket.stomp

public interface StompMessageListener {
    fun onMessage(stompMessage: StompMessage)
}