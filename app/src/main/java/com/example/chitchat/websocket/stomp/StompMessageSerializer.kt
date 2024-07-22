package com.example.chitchat.websocket.stomp

import android.util.Log
import com.example.chitchat.model.Chat
import com.example.chitchat.model.User
import com.example.chitchat.util.Status
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

object StompMessageSerializer {

    private val objectMapper = jacksonObjectMapper()

    fun serialize(message: StompMessage): String {
        val buffer = StringBuilder()

        buffer.append("${message.command}\n")

        for ((key, value) in message.headers) {
            buffer.append("$key:$value\n")
        }

        buffer.append("\n")
        buffer.append(message.body)
        buffer.append('\u0000')

        return buffer.toString()
    }


    fun deserialize(message: String): StompMessage {
        val lines = message.split("\n")

        val command = lines[0].trim()

        val result = StompMessage(command)
        var i = 1
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                break
            }
            val parts = line.split(":", limit = 2)
            val name = parts[0].trim()
            val value = if (parts.size == 2) parts[1].trim() else ""
            result.put(name, value)
            i++
        }

        val sb = StringBuilder()
        while (i < lines.size) {
            sb.append(lines[i])
            i++
        }

        val body = sb.toString().trim()
        result.setContent(body)

        return result
    }


    fun putChatListStompMessageToListOfChats(stompMessage: StompMessage): List<Chat> {
        return try {
            // Deserialize the body of the message to a list of User objects
            Log.d("WebSocket","before deserialize chat is --${stompMessage.body}")
            val chats: List<Chat> = objectMapper.readValue(stompMessage.body)
            Log.d("WebSocket","after deserialize chat is --$chats")
            chats
        } catch (e: Exception) {
            Log.e("WebSocket", "Error deserializing message body",e)
            e.printStackTrace()
            emptyList()
        }
    }


    fun putUserListStompMessageToListOfUsers(stompMessage: StompMessage, currentUser: User): List<User> {
        return try {
            // Deserialize the body of the message to a list of User objects
            val users: List<User> = objectMapper.readValue(stompMessage.body)
            users
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }



}