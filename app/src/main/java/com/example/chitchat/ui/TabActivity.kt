package com.example.chitchat.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chitchat.adapter.ViewPagerAdapter
import com.example.chitchat.databinding.ActivityTabBinding
import com.example.chitchat.model.User
import com.example.chitchat.ui.fragment.ChatFragment
import com.example.chitchat.ui.fragment.UserFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class TabActivity : AppCompatActivity() {
    private var binding:ActivityTabBinding?=null
    private var viewPagerAdapter: ViewPagerAdapter?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        viewPagerAdapter= ViewPagerAdapter(this)

        val bundle2 = intent.extras
        val currentUser: User? = bundle2?.getParcelable("CurrentUserKey")


        val chatFragment = ChatFragment()
        val userFragment = UserFragment()
        val bundle = Bundle()
        bundle.putParcelable("CurrentUserKey", currentUser)
        userFragment.setArguments(bundle)


        viewPagerAdapter!!.addFragment(chatFragment, "chat")
        viewPagerAdapter!!.addFragment(userFragment, "user")

        binding?.viewPager?.adapter=viewPagerAdapter

        val tabLayout: TabLayout = binding?.tabLayout!!
        TabLayoutMediator(tabLayout, binding?.viewPager!!) { tab, position ->
            tab.text = viewPagerAdapter?.getPageTitle(position)
        }.attach()

    }
}