package com.example.chitchat.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.example.chitchat.databinding.FragmentUserBinding
import com.example.chitchat.databinding.UserItemBinding
import com.example.chitchat.model.User
import com.example.chitchat.ui.ChatActivity
import com.example.chitchat.util.Status

class UserAdapter(
    private val users:List<User>,
    private val currentUser:User,
    private val context:Context
): RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: UserItemBinding): RecyclerView.ViewHolder(binding.root) {
        private val userNameTextView=binding.userNameTextView
        private val statusOnImageView=binding.statusOnImageView
        private val statusOffImageview=binding.statusOffImageView

        fun bind(user: User){
            userNameTextView.text=user.userName

            if(user.status==Status.ONLINE){
                statusOnImageView.visibility=View.VISIBLE
                statusOffImageview.visibility=View.GONE
            }
            else if(user.status==Status.OFFLINE){
                statusOnImageView.visibility=View.GONE
                statusOffImageview.visibility=View.VISIBLE
            }

            binding.root.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("CurrentUserKey", currentUser)
                    putExtra("TargetUserKey", user)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(UserItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user:User = users[position]
        holder.bind(user)
    }
}