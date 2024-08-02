package com.example.chitchat.websocket.videosocketclient

import android.util.Log
import com.example.chitchat.config.InternetAddress
import com.example.chitchat.model.MessageModel
import com.example.chitchat.model.TYPE
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

object WebSocketManager :WebSocketListener() {
    private val gson=Gson()
    private lateinit var webSocket: WebSocket
    private var userName:String?=null
    private var activeListener:NewMessageInterface? = null

    fun connect(userName:String,serverUrl:String){
        val client= OkHttpClient()
        val request= Request.Builder()
            .url(serverUrl)
            .build()

        webSocket=client.newWebSocket(request,this)
        client.dispatcher.executorService.shutdown()

        this.userName=userName
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        userName?.let {
            val message=MessageModel(
                TYPE.STORE_USER,userName,null,null
            )
            sendMessageToSocket(message)
        }
        Log.d("VIDEOCALLINGWEBRTC","Connection Establish")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val messageModel=gson.fromJson(text,MessageModel::class.java)
            Log.d("VIDEOCALLINGWEBRTC","onMessage hit for message  ${messageModel.data}")
            Log.d("VIDEOCALLINGWEBRTC","onMessage hit for listener $activeListener")
            activeListener?.onNewMessage(messageModel)
        }
        catch (e:Exception){
            e.printStackTrace()
            Log.d("VIDEOCALLINGWEBRTC", "EXCEPTION during onMessage : ${e.message}")
        }

    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        println("CLOSE: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("VIDEOCALLINGWEBRTC","web socket failure ${t.message}")
    }

    fun setActiveListener(listener: NewMessageInterface?) {
        activeListener = listener
    }


    fun sendMessageToSocket(message:MessageModel){
        try {
            webSocket.send(gson.toJson(message))
        }catch (e:Exception){
            e.printStackTrace()
            Log.d("VIDEOCALLINGWEBRTC", "EXCEPTION during send message to socket : ${e.message}")
        }
    }
}