package com.example.chitchat.websocket.stomp

class TopicHandler(private val topic:String) {
    var listeners:MutableSet<StompMessageListener> = mutableSetOf()


    fun onMessage(stompMessage: StompMessage){
        for (listener in listeners){
            listener.onMessage(stompMessage)
        }
    }

}