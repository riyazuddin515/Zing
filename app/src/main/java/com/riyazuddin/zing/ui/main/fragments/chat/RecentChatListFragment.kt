package com.riyazuddin.zing.ui.main.fragments.chat

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentRecentChatListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecentChatListFragment: Fragment(R.layout.fragment_recent_chat_list) {

    private lateinit var binding: FragmentRecentChatListBinding
    private val args: RecentChatListFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRecentChatListBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.ibNewChat.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable("currentUser", args.currentUser)
            }
            findNavController().navigate(R.id.action_recentChatListFragment_to_newChatFragment, bundle)
        }
    }
}