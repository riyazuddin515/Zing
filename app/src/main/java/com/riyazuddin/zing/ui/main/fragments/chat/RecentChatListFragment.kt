package com.riyazuddin.zing.ui.main.fragments.chat

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.LastMessageAdapter
import com.riyazuddin.zing.databinding.FragmentRecentChatListBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecentChatListFragment : Fragment(R.layout.fragment_recent_chat_list) {

    companion object {
        const val TAG = "RecentChatFrag"
    }

    private lateinit var binding: FragmentRecentChatListBinding
    private val args: RecentChatListFragmentArgs by navArgs()
    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var lastMessageAdapter: LastMessageAdapter

    private var isFirstTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentRecentChatListBinding.inflate(layoutInflater)
        setupRecyclerView()
        viewModel.getLastMessages()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        setupClickListener()

        lastMessageAdapter.setUserSyncListener { chatThread, uid ->
            viewModel.syncLastMessagesOtherUserData(chatThread, uid)
        }

    }

    private fun subscribeToObservers() {
        viewModel.lastMessagesFromRoom.observe(viewLifecycleOwner) {
            lastMessageAdapter.lastMessages = it
            lastMessageAdapter.notifyDataSetChanged()
        }
        viewModel.isLastMessagesFirstLoadDone.observe(viewLifecycleOwner, EventObserver {
            if (it and isFirstTime) {
                isFirstTime = false
                viewModel.setLastMessageListener(args.currentUser)
            }
        })
    }

    private fun setupClickListener() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.ibNewChat.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable("currentUser", args.currentUser)
            }
            findNavController().navigate(
                R.id.action_recentChatListFragment_to_newChatFragment,
                bundle
            )
        }

        lastMessageAdapter.setOnItemClickListener { lastMessage ->
            val bundle = Bundle().apply {
                putSerializable("currentUser", args.currentUser)
                putSerializable("otherEndUser", lastMessage.otherUser)
            }
            findNavController().navigate(R.id.action_recentChatListFragment_to_chatFragment, bundle)
        }
    }

    private fun setupRecyclerView() {
        binding.rvRecentChatList.apply {
            adapter = lastMessageAdapter
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onStart() {
        super.onStart()
        val notificationManager: NotificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.removeLastMessageListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }
}