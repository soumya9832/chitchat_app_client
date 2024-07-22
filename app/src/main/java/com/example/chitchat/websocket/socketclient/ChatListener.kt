package com.example.chitchat.websocket.socketclient

import android.util.Log
import com.example.chitchat.model.ChatEndpoint
import com.example.chitchat.websocket.stomp.CloseHandler
import okhttp3.Response
import okhttp3.WebSocket

class ChatListener(
    private val chatEndPoint: ChatEndpoint
) : SpringBootWebSocketClient(){
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WebSocket10","onOpen of chatListener")
        sendConnectMessage(webSocket)

        for((topic,handler) in topicHandlers){
            sendSubscribeMessage(webSocket,topic)
        }

        for ((queue,handler) in queueHandlers){
            Log.d("WebSocket10","chatListener subscribe to queue $queue")
            sendSubscribeMessage(webSocket,queue)
        }

        val destination = "/app/chat/listen"
        sendChatEndpoint(webSocket,destination,chatEndPoint)
        closeHandler = CloseHandler(webSocket)
    }
}