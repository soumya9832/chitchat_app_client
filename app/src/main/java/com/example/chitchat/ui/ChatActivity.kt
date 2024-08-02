package com.example.chitchat.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chitchat.adapter.ChatAdapter
import com.example.chitchat.config.InternetAddress
import com.example.chitchat.databinding.ActivityChatBinding
import com.example.chitchat.model.Chat
import com.example.chitchat.model.ChatEndpoint
import com.example.chitchat.model.MessageModel
import com.example.chitchat.model.TYPE
import com.example.chitchat.model.User
import com.example.chitchat.websocket.socketclient.ChatDeliver
import com.example.chitchat.websocket.socketclient.ChatListener
import com.example.chitchat.websocket.stomp.StompMessage
import com.example.chitchat.websocket.stomp.StompMessageListener
import com.example.chitchat.websocket.stomp.StompMessageSerializer
import com.example.chitchat.websocket.videosocketclient.NewMessageInterface
import com.example.chitchat.websocket.videosocketclient.WebSocketManager
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity(),NewMessageInterface {
    private var binding: ActivityChatBinding? = null
    private lateinit var senderId: String
    private lateinit var receiverId: String
    private lateinit var chatListener: ChatListener
    private lateinit var chatDeliver: ChatDeliver


    private var currentUser: User? = null
    private var targetUser: User? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        getPermissionsForVideoCall(this)

        currentUser = intent.getParcelableExtra("CurrentUserKey")
        targetUser = intent.getParcelableExtra("TargetUserKey")

        if (currentUser != null && targetUser != null) {
            senderId = currentUser!!.userName
            receiverId = targetUser!!.userName
            Log.d("WebSocket", "sender--$senderId receiver--$receiverId")
        }

        binding?.targetUserNameTextView?.text = receiverId


        val toolbar = binding?.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar?.setNavigationOnClickListener {
            chatListener.disconnect()
            if (::chatDeliver.isInitialized) {
                chatDeliver.disconnect()
            }
            finish()
        }


        binding?.videoCallBtn?.setOnClickListener {

            PermissionX.init(this)
                .permissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ).request { allGranted, _, _ ->
                    if (allGranted) {

                        intent=Intent(applicationContext,VideoActivity::class.java)
                        intent.putExtra("CurrentUserKey",currentUser)
                        intent.putExtra("TargetUserKey",targetUser)
                        startActivity(intent)

                    } else {
                        Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG)
                            .show()
                    }

                }
        }






        binding?.recyclerView?.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        binding?.recyclerView?.layoutManager = linearLayoutManager


        val chatEndPoint = ChatEndpoint(senderId, receiverId)


        chatListener = ChatListener(chatEndPoint)
        val queueHandler = chatListener.subscribeToQueue("/user/${receiverId}/queue/messages")

        queueHandler.listener = object : StompMessageListener {
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
                        Log.d("WebSocket10", "Chat listener listen chat ${chats[0].content}")
                        val messageAdapter = ChatAdapter(chats, chatEndPoint.senderId)
                        binding?.recyclerView?.adapter = messageAdapter
                    }
                }

        }


        chatListener.connect(InternetAddress.webSocketAddress)




        binding?.messageSendButton?.setOnClickListener {
            val message = binding?.messageEditText?.text.toString()
            if (message == "") {
                Toast.makeText(this, "you can't send empty message", Toast.LENGTH_SHORT).show()
            } else {
                val chat = Chat(
                    senderId = senderId,
                    recipientId = receiverId,
                    content = message
                )
                chatDeliver = ChatDeliver(chat)
                val queueHandler = chatDeliver.subscribeToQueue("/user/${receiverId}/queue/messages")

                queueHandler.listener = object : StompMessageListener {
                    override fun onMessage(stompMessage: StompMessage) {
                        runOnUiThread {
                            if (stompMessage.body == "[]") {
                                return@runOnUiThread
                            }

                            val chats = StompMessageSerializer.putChatListStompMessageToListOfChats(stompMessage)
                            Log.d("WebSocket10", "Chat deliver  chat ${chats[0].content}")
                            val messageAdapter = ChatAdapter(chats, chatEndPoint.senderId)
                            binding?.recyclerView?.adapter = messageAdapter
                        }
                    }

                }
                chatDeliver.connect(InternetAddress.webSocketAddress)
            }
            binding?.messageEditText?.setText("")
        }



    }

    override fun onResume() {
        super.onResume()
        WebSocketManager.setActiveListener(this)
        val chatDeliverInit=::chatDeliver.isInitialized
        val chatListener=::chatListener.isInitialized
    }

    override fun onPause() {
        super.onPause()
        WebSocketManager.setActiveListener(null)
        Log.d("VIDEOCALLWEBRTC","onPause hit of chat activity...")
    }


    override fun onNewMessage(message: MessageModel) {
        Log.d("VIDEOCALLINGWEBRTC","onNewMessage of ChatActivity hit....message type---${message.type}")

        when(message.type){
            TYPE.OFFER_RECEIVED->{
                Log.d("VIDEOCALLINGWEBRTC", "Recived")
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        showIncomingCallLayout()
                        binding?.incomingName?.text = "${message.name.toString()} is calling you"

                        // Accept button for accepting the call
                        binding?.acceptButton?.setOnClickListener {
                            Log.d("VIDEOCALLINGWEBRTC","accept button hit...")
                            hideIncomingCallLayout()

                            Log.d("VIDEOCALLINGWEBRTC","data value of message is --${message.data}")

                            val intent = Intent(this@ChatActivity,VideoCallResponseActivity::class.java)
                            intent.putExtra("sessionDescription", message.data.toString())
                            intent.putExtra("remoteUserId", message.name)
                            intent.putExtra("localUserId",senderId)
                            startActivity(intent)



                        }
                        /*
                        If the user presses the reject button, a CALL_ENDED
                        message is sent to the WebSocket, notifying the
                        remote peer that the call has ended.
                        */
                        binding?.rejectButton?.setOnClickListener {
                            Log.d("VIDEOCALLINGWEBRTC","reject button hit...")
                            hideIncomingCallLayout()
                            val myMessage = MessageModel(TYPE.CALL_ENDED, currentUser?.userName, message.name, null)
                            WebSocketManager.sendMessageToSocket(myMessage)
                        }

                    }
                }
            }
            else->{

            }
        }
    }

    private fun showIncomingCallLayout(){
        binding?.main?.isFocusable=false
        binding?.main?.alpha= 0.5f
        binding?.incomingCallLayout?.visibility=View.VISIBLE
        binding?.incomingCallLayout?.bringToFront()
        binding?.incomingCallLayout?.requestFocus()
    }

    private fun hideIncomingCallLayout(){
        binding?.main?.isFocusable=true
        binding?.main?.alpha=1.0f
        binding?.incomingCallLayout?.visibility=View.GONE
    }


    // Here we are requesting the necessary permissions.
    private fun getPermissionsForVideoCall(activity:ChatActivity) {
        PermissionX.init(activity)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ).request { allGranted, _, _ ->
                if (allGranted) {


                } else {
                    Toast.makeText(activity, "you should accept all permissions", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("VIDEOCALLINGWEBRTC","onDestroy hit of chat activity...")
    }




}
