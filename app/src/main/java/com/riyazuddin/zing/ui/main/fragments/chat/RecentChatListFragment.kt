package com.riyazuddin.zing.ui.main.fragments.chat

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.LastMessageAdapter
import com.riyazuddin.zing.databinding.FragmentRecentChatListBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.SEEN
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecentChatListFragment : Fragment(R.layout.fragment_recent_chat_list) {

    private lateinit var binding: FragmentRecentChatListBinding
    private val args: RecentChatListFragmentArgs by navArgs()

    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var lastMessageAdapter: LastMessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentRecentChatListBinding.inflate(layoutInflater)

        Log.i(TAG, "onCreate: ")
        setupRecyclerView()
        viewModel.getLastMessageFirstQuery(args.currentUser)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        subscribeToObservers()

        lastMessageAdapter.setOnItemClickListener { lastMessage, position ->
            lastMessageAdapter.lastMessages[position].message.status = SEEN
            lastMessageAdapter.notifyItemChanged(position)
            val bundle = Bundle().apply {
                putSerializable("currentUser", args.currentUser)
                if (lastMessage.sender.uid == args.currentUser.uid)
                    putSerializable("otherEndUser", lastMessage.receiver)
                else
                    putSerializable("otherEndUser", lastMessage.sender)
            }
            findNavController().navigate(R.id.action_recentChatListFragment_to_chatFragment, bundle)
        }
    }

    private fun subscribeToObservers() {
        viewModel.recentMessagesList.observe(viewLifecycleOwner, EventObserver(
            onError = {
                if (it != Constants.NO_MORE_MESSAGES) {
                    snackBar(it)
                }
                binding.progressBar.isVisible = false
            },
            onLoading = {
                Log.i(TAG, "subscribeToObservers: Loading")
                binding.progressBar.isVisible = true
            }
        ) {
            lastMessageAdapter.lastMessages = it
            binding.progressBar.isVisible = false
            lastMessageAdapter.notifyDataSetChanged()
        })
        viewModel.isLastMessagesFirstLoadDone.observe(viewLifecycleOwner, EventObserver {
            if (it)
                viewModel.setLastMessageListener(args.currentUser)
        })
    }

    private fun setupRecyclerView() {
        binding.rvRecentChatList.apply {
            adapter = lastMessageAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val pos = layoutManager.findLastCompletelyVisibleItemPosition()
                    val numItems: Int = recyclerView.adapter!!.itemCount

                    Log.i(TAG, "onScrolled: pos = $pos ----- numItem = $numItems")

                    if (pos + 1 == numItems) {
                        viewModel.getLastMessageLoadMore(args.currentUser)
                        Log.i(TAG, "onScrolled: calling getChatLoadMore")
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        viewModel.clearRecentMessagesList()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        //Removing all existing notification as soon as activity starts
        val notificationManager: NotificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    companion object {
        const val TAG = "RecentChatFrag"
    }
}