package com.riyazuddin.zing.ui.main.fragments.chat

import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.ChatAdapter
import com.riyazuddin.zing.databinding.FragmentChatBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.log

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding

    private val viewModel: ChatViewModel by viewModels()

    private val args: ChatFragmentArgs by navArgs()

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var chatAdapter: ChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack(R.id.recentChatListFragment, false)
        }

        glide.load(args.otherEndUser.profilePicUrl).into(binding.CIVProfilePic)
        binding.tvUsername.text = args.otherEndUser.username

        setUpRecyclerView()
        subscribe()

        viewModel.getChatLoadFirstQuery(args.currentUser.uid, args.otherEndUser.uid)

        binding.btnSend.setOnClickListener {
            viewModel.sendMessage(

                currentUid = Firebase.auth.uid!!,
                receiverUid = args.otherEndUser.uid,
                message = binding.TIEMessage.text.toString(),
                type = Constants.TEXT,
                uri = null,

                senderName = args.currentUser.name,
                senderUsername = args.currentUser.username,
                senderProfilePicUrl = args.currentUser.profilePicUrl,

                receiverName = args.otherEndUser.name,
                receiverUsername = args.otherEndUser.username,
                receiveProfileUrl = args.otherEndUser.profilePicUrl,
            )
        }

        chatAdapter.setOnItemLongClickListener { message, position ->
            CustomDialog(
                "Delete this message?",
                "Are you sure you want to delete this message?"
            ).apply {
                setPositiveListener {
                    viewModel.deleteMessage(args.currentUser.uid, args.otherEndUser.uid, message)
                    chatAdapter.messages[position].message = "Deleting...."
                    chatAdapter.notifyItemChanged(position)
                }
            }.show(parentFragmentManager, null)
        }
    }

    private fun subscribe() {
        viewModel.sendMessageStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                Log.e(TAG, "subscribe: $it")
                snackBar(it)
            },
            onLoading = {
                Log.i(TAG, "subscribe: Sending message")
            }
        ) {
            Log.d(TAG, "subscribe() message send success: $it")
            binding.TIEMessage.text?.clear()
        })
        viewModel.chatList.observe(viewLifecycleOwner, EventObserver{
            it.forEach { message ->
                Log.i(TAG, "subscribeToObserver: ${message.message} ")
            }
            chatAdapter.messages = it
            chatAdapter.notifyDataSetChanged()
        })
    }

    private fun setUpRecyclerView() {
        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true
                stackFromEnd = false
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val pos = layoutManager.findLastCompletelyVisibleItemPosition()
                    val numItems: Int = recyclerView.adapter!!.itemCount

                    if (pos + 1 == numItems) {
                        viewModel.getChatLoadMore(args.currentUser.uid, args.otherEndUser.uid)
                    }
                }
            })
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom) {
                    binding.rvChat.smoothScrollToPosition(0)
                }
            }
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: ")
//        chatAdapter.messages = listOf()
        viewModel.clearChatList()
        super.onDestroy()
    }

    companion object {
        const val TAG = "ChatFragment"
    }

}