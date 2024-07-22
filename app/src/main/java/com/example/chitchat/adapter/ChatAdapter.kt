package com.example.chitchat.adapter


import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.chitchat.databinding.ChatItemLeftBinding
import com.example.chitchat.databinding.ChatItemRightBinding
import com.example.chitchat.model.Chat
import com.example.chitchat.model.User


class ChatAdapter(private val chats: List<Chat>,
                  private val currentUsername: String,
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        const val CURRENT_USER_TYPE = 0
        const val TARGET_USER_TYPE = 1
    }

    inner class ChatViewHolder(private val binding: ViewBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(chat: Chat){
            when(binding){
                is ChatItemLeftBinding->{
                    binding.chatItemMessageTextView.text=chat.content
                    Log.d("WebSocket20","chat left item ${chat.content}")
                }
                is ChatItemRightBinding->{
                    binding.chatItemMessageTextView.text=chat.content
                    Log.d("WebSocket20","chat right item ${chat.content}")
                }
            }
        }


    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return if(viewType== CURRENT_USER_TYPE){
            ChatViewHolder(ChatItemRightBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        } else{
            ChatViewHolder(ChatItemLeftBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int {
        return chats.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(chats[position].senderId == currentUsername){
            CURRENT_USER_TYPE
        } else{
            TARGET_USER_TYPE
        }
    }
}