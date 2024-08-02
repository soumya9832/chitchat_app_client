package com.example.chitchat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chitchat.adapter.ViewPagerAdapter
import com.example.chitchat.config.InternetAddress
import com.example.chitchat.databinding.ActivityTabBinding
import com.example.chitchat.model.User
import com.example.chitchat.ui.fragment.StatusFragment
import com.example.chitchat.ui.fragment.UserFragment
import com.example.chitchat.websocket.videosocketclient.WebSocketManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class TabActivity : AppCompatActivity() {
    private var binding:ActivityTabBinding?=null
    private var viewPagerAdapter: ViewPagerAdapter?=null
    private lateinit var userName:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        viewPagerAdapter= ViewPagerAdapter(this)

        val bundle2 = intent.extras
        val currentUser: User? = bundle2?.getParcelable("CurrentUserKey")
        if(currentUser!=null){
            userName=currentUser.userName
        }



        val statusFragment = StatusFragment()
        val userFragment = UserFragment()
        val bundle = Bundle()
        bundle.putParcelable("CurrentUserKey", currentUser)
        userFragment.setArguments(bundle)


        viewPagerAdapter!!.addFragment(userFragment, "user")
        viewPagerAdapter!!.addFragment(statusFragment, "status")

        binding?.viewPager?.adapter=viewPagerAdapter

        val tabLayout: TabLayout = binding?.tabLayout!!
        TabLayoutMediator(tabLayout, binding?.viewPager!!) { tab, position ->
            tab.text = viewPagerAdapter?.getPageTitle(position)
        }.attach()


        WebSocketManager.connect(userName,InternetAddress.videoCallServer)

    }
}