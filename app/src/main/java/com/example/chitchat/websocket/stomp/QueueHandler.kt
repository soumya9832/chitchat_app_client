package com.example.chitchat.websocket.stomp

class QueueHandler(private val queue:String) {
    var listener:StompMessageListener?=null

    fun onMessage(message: StompMessage){
        listener?.onMessage(message)
    }

}