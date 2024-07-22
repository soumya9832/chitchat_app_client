package com.example.chitchat.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchat.Hola
import com.example.chitchat.R
import com.example.chitchat.adapter.UserAdapter
import com.example.chitchat.config.InternetAddress
import com.example.chitchat.model.User
import com.example.chitchat.websocket.socketclient.UserListListener
import com.example.chitchat.websocket.stomp.StompMessage
import com.example.chitchat.websocket.stomp.StompMessageListener
import com.example.chitchat.websocket.stomp.StompMessageSerializer

class UserFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var userList: List<User>
    private lateinit var userListListener: UserListListener
    private lateinit var currentUser:User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Get extras
        val tempUser = arguments?.getParcelable<User>("CurrentUserKey")
        if(tempUser != null){
            currentUser= tempUser
        }
        Log.d("WebSocket","currentuser $currentUser")

        // Get extras ends


        userListListener = UserListListener()
        val topicHandler = userListListener.subscribeToTopic("/topic/userList")
            topicHandler.listeners.add(object : StompMessageListener {
                override fun onMessage(stompMessage: StompMessage) {
                    activity?.runOnUiThread {

                        userList= listOf()

                        userList = StompMessageSerializer.putUserListStompMessageToListOfUsers(stompMessage,currentUser)
                        userAdapter = UserAdapter(userList, currentUser, requireContext())
                        recyclerView.adapter = userAdapter
                    }
                }
            })

        userListListener.connect(InternetAddress.webSocketAddress)
        return view
    }


}