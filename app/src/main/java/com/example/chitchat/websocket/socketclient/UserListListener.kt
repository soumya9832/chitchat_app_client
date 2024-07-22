package com.example.chitchat.websocket.socketclient

import android.util.Log
import com.example.chitchat.websocket.stomp.CloseHandler
import okhttp3.Response
import okhttp3.WebSocket

class UserListListener:SpringBootWebSocketClient() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WebSocket","onOpen")
        //super.onOpen(webSocket, response)
        sendConnectMessage(webSocket)

        for((topic,handler) in topicHandlers){
            Log.d("WebSocket","topic --- $topic")
            sendSubscribeMessage(webSocket,topic)
        }
        for((queue,handler) in queueHandlers){
            sendSubscribeMessage(webSocket,queue)
        }

        val destination = "/app/userList/status/listen"
        sendMessage(webSocket,destination,"")


        closeHandler = CloseHandler(webSocket)
    }


}