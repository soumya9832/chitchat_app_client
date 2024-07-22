package com.example.chitchat.websocket.stomp

import okhttp3.WebSocket

class CloseHandler(private val webSocket:WebSocket) {

    fun close(){
        webSocket.close(1000,"close websocket")
    }

}