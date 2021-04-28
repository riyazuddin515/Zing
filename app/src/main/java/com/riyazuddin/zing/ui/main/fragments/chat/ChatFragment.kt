package com.riyazuddin.zing.ui.main.fragments.chat

import android.media.MediaPlayer
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

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()

    private val args: ChatFragmentArgs by navArgs()

    private lateinit var mediaPlayer: MediaPlayer

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.received_chat)

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
        ) { sentMessage ->
            Log.d(TAG, "subscribe() message send success: $sentMessage")
            viewModel.updateChatListOnMessageSent(sentMessage)
            binding.TIEMessage.text?.clear()
        })

        viewModel.chatList.observe(viewLifecycleOwner, EventObserver(
            onError = {
                    snackBar(it)
                    binding.linearProgressIndicator.isVisible = false
            },
            onLoading = {
                Log.i(TAG, "subscribe: loading")
                binding.linearProgressIndicator.isVisible = true
            }
        ) {
            it.forEach { message ->
                Log.i(TAG, "subscribeToObserver it: ${message.message} ")
            }
            binding.linearProgressIndicator.isVisible = false
            chatAdapter.messages = it
            chatAdapter.notifyDataSetChanged()
        })
        viewModel.playTone.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true
        ) {
            if (it) {
                mediaPlayer.start()
            }
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

                    Log.i(TAG, "onScrolled: pos = $pos ----- numItem = $numItems")


                    if (pos + 1 == numItems) {
                        viewModel.getChatLoadMore(args.currentUser.uid, args.otherEndUser.uid)
                        Log.i(TAG, "onScrolled: calling getChatLoadMore")
//                        isLoadingMore = true
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

    override fun onDestroyView() {
        viewModel.clearChatList()
        viewModel.chatList.removeObservers(viewLifecycleOwner)
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ChatFragment"
    }

}