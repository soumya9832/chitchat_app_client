package com.example.chitchat.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chitchat.R
import com.example.chitchat.databinding.ActivityMainBinding
import com.example.chitchat.model.User
import com.example.chitchat.retrofit.RetrofitClient
import com.example.chitchat.util.Status
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.userNameEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed before text changed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val userNameString = binding?.userNameEditText?.text.toString().trim()
                if (userNameString.isEmpty()) {
                    binding?.chatButton?.setBackgroundResource(R.drawable.rounded_button_grey)
                    binding?.chatButton?.isEnabled = false
                } else {
                    binding?.chatButton?.setBackgroundResource(R.drawable.rounded_button_blue)
                    binding?.chatButton?.isEnabled = true
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // No action needed after text changed
            }
        })


        binding?.chatButton?.setOnClickListener {
            val user = User(0,binding?.userNameEditText?.text.toString().trim(),"", Status.ONLINE)
            addUser(user)
        }






    }

    private fun startTabActivity(id: Int) {
        val user = User(
            id,
            binding?.userNameEditText?.text.toString().trim(),
            "",
            Status.ONLINE
        )

        val intent = Intent(applicationContext, TabActivity::class.java)
        val bundle = Bundle().apply {
            putParcelable("CurrentUserKey",user)
        }
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun addUser(user:User) {
        val call = RetrofitClient.api.addUser(user)
        call.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                if(response.isSuccessful){
                    val addedUser = response.body();
                    try {
                        if(addedUser?.userName.equals("userName Taken")){
                            binding?.userNameTakenTextView?.visibility = View.VISIBLE
                        }
                        else{
                            binding?.userNameTakenTextView?.visibility = View.INVISIBLE
                            Toast.makeText(this@MainActivity,"User Added Successfully",Toast.LENGTH_SHORT).show()
                            if (addedUser != null) {
                                startTabActivity(addedUser.id)
                            }
                        }
                    }catch (e: JSONException){
                        e.printStackTrace()
                    }
                }
                else{
                    Toast.makeText(this@MainActivity,"Response Creation problem in Add User Task",Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Toast.makeText(this@MainActivity,"Failed To Add User",Toast.LENGTH_SHORT).show()
                Log.e("Retrofit", "Request Failure: ${t.message}")
            }

        })

    }
}