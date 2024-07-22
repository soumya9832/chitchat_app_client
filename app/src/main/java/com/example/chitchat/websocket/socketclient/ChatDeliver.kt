package com.example.chitchat.websocket.socketclient

import android.util.Log
import com.example.chitchat.model.Chat
import com.example.chitchat.websocket.stomp.CloseHandler
import okhttp3.Response
import okhttp3.WebSocket

class ChatDeliver(
    private val chat:Chat
): SpringBootWebSocketClient() {

    override fun onOpen(webSocket: WebSocket, response: Response) {

        Log.d("WebSocket10","onOpen of chatDeliver")
        sendConnectMessage(webSocket)

        for((topic,handler) in topicHandlers){
            sendSubscribeMessage(webSocket,topic)
        }

        for ((queue,handler) in queueHandlers){
            Log.d("WebSocket10","chatDeliver subscribe to queue $queue")
            sendSubscribeMessage(webSocket,queue)
        }

        val destination = "/app/chat"
        sendChatMessage(webSocket,destination,chat)
        closeHandler = CloseHandler(webSocket)
    }
}