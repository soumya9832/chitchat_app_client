package com.example.chitchat.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chitchat.adapter.ChatAdapter
import com.example.chitchat.config.InternetAddress
import com.example.chitchat.databinding.ActivityChatBinding
import com.example.chitchat.model.Chat
import com.example.chitchat.model.ChatEndpoint
import com.example.chitchat.model.User
import com.example.chitchat.websocket.socketclient.ChatDeliver
import com.example.chitchat.websocket.socketclient.ChatListener
import com.example.chitchat.websocket.stomp.StompMessage
import com.example.chitchat.websocket.stomp.StompMessageListener
import com.example.chitchat.websocket.stomp.StompMessageSerializer
import com.google.android.material.appbar.MaterialToolbar

class ChatActivity : AppCompatActivity() {
    private var binding:ActivityChatBinding?=null
    private lateinit var senderId:String
    private lateinit var receiverId:String
    private lateinit var chatListener: ChatListener
    private lateinit var chatDeliver: ChatDeliver

    private  var currentUser: User?=null
    private  var targetUser: User?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        currentUser=intent.getParcelableExtra("CurrentUserKey")
        targetUser=intent.getParcelableExtra("TargetUserKey")

        if(currentUser!=null && targetUser!=null){
            senderId= currentUser!!.userName
            receiverId=targetUser!!.userName
            Log.d("WebSocket","sender--$senderId receiver--$receiverId")
        }

        binding?.targetUserNameTextView?.text=receiverId

        val toolbar = binding?.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title=""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar?.setNavigationOnClickListener{
            chatListener.disconnect()
            if(::chatDeliver.isInitialized){
                chatDeliver.disconnect()
            }
            finish()
        }






        binding?.recyclerView?.setHasFixedSize(true)
        val linearLayoutManager=LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd=true
        binding?.recyclerView?.layoutManager=linearLayoutManager





        val chatEndPoint=ChatEndpoint(senderId,receiverId)


        chatListener = ChatListener(chatEndPoint)
        val queueHandler = chatListener.subscribeToQueue("/user/${receiverId}/queue/messages")

        queueHandler.listener= object :StompMessageListener{
            override fun onMessage(stompMessage: StompMessage) {
                runOnUiThread {
                    if (stompMessage.body == "[]") {
                        return@runOnUiThread
                    }

                    val chats = StompMessageSerializer.putChatListStompMessageToListOfChats(stompMessage)
                    if (chats.isEmpty()) {
                        // Handle empty list case if needed
                        Log.d("WebSocket10", "No chats found")
                        return@runOnUiThread
                    }
                    Log.d("WebSocket10","Chat listener listen chat ${chats[0].content}")
                    val messageAdapter = ChatAdapter(chats,chatEndPoint.senderId)
                    binding?.recyclerView?.adapter = messageAdapter
                }
            }

        }


        chatListener.connect(InternetAddress.webSocketAddress)




        binding?.messageSendButton?.setOnClickListener {
            val message = binding?.messageEditText?.text.toString()
            if(message == ""){
                Toast.makeText(this,"you can't send empty message",Toast.LENGTH_SHORT).show()
            }
            else{
                val chat = Chat(
                    senderId = senderId,
                    recipientId = receiverId,
                    content = message
                )
                chatDeliver = ChatDeliver(chat)
                val queueHandler = chatDeliver.subscribeToQueue("/user/${receiverId}/queue/messages")

                queueHandler.listener= object :StompMessageListener{
                    override fun onMessage(stompMessage: StompMessage) {
                        runOnUiThread {
                            if (stompMessage.body == "[]") {
                                return@runOnUiThread
                            }

                            val chats = StompMessageSerializer.putChatListStompMessageToListOfChats(stompMessage)
                            Log.d("WebSocket10","Chat deliver  chat ${chats[0].content}")
                            val messageAdapter = ChatAdapter(chats,chatEndPoint.senderId)
                            binding?.recyclerView?.adapter = messageAdapter
                        }
                    }

                }
                chatDeliver.connect(InternetAddress.webSocketAddress)
            }
            binding?.messageEditText?.setText("")
        }


    }
}