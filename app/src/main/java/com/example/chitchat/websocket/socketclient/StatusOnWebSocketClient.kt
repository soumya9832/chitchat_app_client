package com.example.chitchat.websocket.socketclient

import com.example.chitchat.websocket.stomp.CloseHandler
import okhttp3.Response
import okhttp3.WebSocket

class StatusOnWebSocketClient(
    private val userId:String
):SpringBootWebSocketClient() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        sendConnectMessage(webSocket)

        for((topic,handler) in topicHandlers){
            sendSubscribeMessage(webSocket,topic)
        }
        for((queue,handler) in queueHandlers){
            sendSubscribeMessage(webSocket,queue)
        }

        val destination = "/app/userList/status/on"
        sendMessage(webSocket,destination,userId)
        closeHandler = CloseHandler(webSocket)
    }
}