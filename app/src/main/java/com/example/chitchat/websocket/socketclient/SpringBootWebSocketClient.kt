package com.example.chitchat.websocket.socketclient

import android.util.Log
import com.example.chitchat.model.Chat
import com.example.chitchat.model.ChatEndpoint
import com.example.chitchat.websocket.stomp.CloseHandler
import com.example.chitchat.websocket.stomp.QueueHandler
import com.example.chitchat.websocket.stomp.StompMessage
import com.example.chitchat.websocket.stomp.StompMessageSerializer
import com.example.chitchat.websocket.stomp.TopicHandler
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

open class SpringBootWebSocketClient : WebSocketListener() {
    val queueHandlers = mutableMapOf<String,QueueHandler>()
    val topicHandlers = mutableMapOf<String,TopicHandler>()

    var webSocket:WebSocket? = null
    var closeHandler:CloseHandler? = null

    public val id:String = "sub-001"

    fun connect(serverUrl:String){
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MICROSECONDS)
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        client.newWebSocket(request,this)
        client.dispatcher.executorService.shutdown()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket=webSocket
        sendConnectMessage(webSocket)

        for((queue,handler) in queueHandlers){
            sendSubscribeMessage(webSocket,queue)
        }

        for((topic,handler) in topicHandlers){
            sendSubscribeMessage(webSocket,topic)
        }

        closeHandler = CloseHandler(webSocket)
    }

    fun sendConnectMessage(webSocket:WebSocket){
        val message = StompMessage(
            command = "CONNECT",
            headers = mutableMapOf("accept-version" to "1.1", "heart-beat" to "10000,10000")
        )
        webSocket.send(StompMessageSerializer.serialize(message))
    }

    fun sendSubscribeMessage(webSocket:WebSocket,destination:String){
        val message = StompMessage(
            command = "SUBSCRIBE",
            headers = mutableMapOf("destination" to destination,"id" to id)
        )

        webSocket.send(StompMessageSerializer.serialize(message))

    }

    fun subscribeToQueue(queue:String):QueueHandler{
        val handler = QueueHandler(queue)
        queueHandlers[queue]=handler
        return handler
    }

    fun subscribeToTopic(topic:String):TopicHandler{
        Log.d("WebSocket","subscribe to topic")
        val handler = TopicHandler(topic)
        topicHandlers[topic]=handler
        return handler
    }

    fun unSubscribeToQueue(queue: String){
        queueHandlers.remove(queue)
    }

    fun unSubscribeToTopic(topic:String){
        topicHandlers.remove(topic)
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }

    fun disconnect(){
        if(webSocket!=null){
            closeHandler?.close()
            webSocket=null
            closeHandler=null
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        val message:StompMessage = StompMessageSerializer.deserialize(text)
        Log.d("WebSocket", "stomp message body: ${message.body}")
        if(message.command=="CONNECTED"){
            // Handle connection established message
            Log.d("WebSocket", "Connected successfully")
            return
        }
        val destination: String? = message.getHeader("destination")
        Log.d("WebSocket", "Message came for $destination")

        if (destination != null) {
            // Route the message to the appropriate handler
            queueHandlers[destination]?.onMessage(message)
            topicHandlers[destination]?.onMessage(message)
        } else {
            Log.d("WebSocket", "No destination header in message")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        println("CLOSE: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        Log.e("WebSocket","web socket failure ${t.message}")
    }

    fun sendMessage(webSocket: WebSocket,destination: String, content: String) {
        val message = StompMessage(
            command = "SEND",
            headers = mutableMapOf("destination" to destination),
            body = content
        )
        Log.d("WebSocket","send message hit")
        webSocket.send(StompMessageSerializer.serialize(message))
    }

    fun sendChatMessage(webSocket: WebSocket,destination: String,chat:Chat){
        val message = StompMessage(
            command = "SEND",
            headers = mutableMapOf("destination" to destination),
            body = Gson().toJson(chat)
        )
        webSocket.send(StompMessageSerializer.serialize(message))
    }

    fun sendChatEndpoint(webSocket: WebSocket,destination: String,chatEndpoint: ChatEndpoint){
        val message = StompMessage(
            command = "SEND",
            headers = mutableMapOf("destination" to destination),
            body = Gson().toJson(chatEndpoint)
        )
        webSocket.send(StompMessageSerializer.serialize(message))
    }





}