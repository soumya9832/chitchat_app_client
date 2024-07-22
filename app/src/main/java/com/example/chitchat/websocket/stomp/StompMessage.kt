package com.example.chitchat.websocket.stomp


data class StompMessage(
    val command:String,
    val headers:MutableMap<String,String> = mutableMapOf(),
    var body:String=""
){
    fun put(key:String,value:String){
        headers[key]=value
    }

    fun getHeader(key:String):String?=headers[key]

    fun setContent(body:String){
        this.body=body
    }

    fun getContent():String = body
}