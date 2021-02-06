package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.ChatListAdapter
import com.riyazuddin.zing.data.ChatFragmentData
import com.riyazuddin.zing.databinding.FragmentChatListBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private lateinit var binding: FragmentChatListBinding

    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var glide: RequestManager

    private var firestoreChatListAdapter: ChatListAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatListBinding.bind(view)

        viewModel.getLastMessages(Firebase.auth.uid!!)
        subscribeToObservers()

        binding.ivNewChat.setOnClickListener {
            findNavController().navigate(R.id.action_chatList_to_newChat)
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun subscribeToObservers() {
        viewModel.lastMessagesOptions.observe(viewLifecycleOwner, EventObserver(

        ) {
            firestoreChatListAdapter = ChatListAdapter(it, glide)
            firestoreChatListAdapter!!.startListening()
            setupRecyclerView()
            firestoreChatListAdapter!!.setOnItemClickListener { uid, name, username, profilePicUrl ->
                val bundle = Bundle().apply {
                    putSerializable(
                        "chatFragmentData",
                        ChatFragmentData(uid, name, username, profilePicUrl)
                    )
                }
                findNavController().navigate(R.id.action_chatList_to_chat, bundle)
            }
        })
    }


    private fun setupRecyclerView() {

        binding.rvChatLast.apply {
            adapter = firestoreChatListAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreChatListAdapter?.stopListening()
    }
}