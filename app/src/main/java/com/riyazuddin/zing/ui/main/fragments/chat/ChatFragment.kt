package com.riyazuddin.zing.ui.main.fragments.chat

import android.app.Application
import android.app.NotificationManager
import android.content.Context
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
import com.riyazuddin.zing.other.Constants.CHATTING_WITH
import com.riyazuddin.zing.other.Constants.LAST_SEEN
import com.riyazuddin.zing.other.Constants.NOTIFICATION_ID
import com.riyazuddin.zing.other.Constants.NO_MORE_MESSAGES
import com.riyazuddin.zing.other.Constants.NO_ONE
import com.riyazuddin.zing.other.Constants.ONLINE
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.fragments.HomeFragmentDirections
import com.riyazuddin.zing.ui.main.fragments.OthersProfileFragmentDirections
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding

    private val viewModel: ChatViewModel by viewModels()

    private val args: ChatFragmentArgs by navArgs()

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var simpleDateFormat: SimpleDateFormat

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentChatBinding.inflate(layoutInflater)

        setUpRecyclerView()

        viewModel.checkUserIsOnline(args.otherEndUser.uid)
        viewModel.getChatLoadFirstQuery(args.currentUser.uid, args.otherEndUser.uid)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sp = requireActivity().getSharedPreferences(CHATTING_WITH, Application.MODE_PRIVATE)
        sp.edit().let {
            it.putString(Constants.UID, args.otherEndUser.uid)
            it.apply()
        }

        //Removing all existing notification as soon as activity starts
        val notificationManager: NotificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(args.otherEndUser.uid, NOTIFICATION_ID)

        simpleDateFormat = SimpleDateFormat("d MMM yyyy HH:mm a", Locale.ENGLISH)
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.received_chat)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack(R.id.recentChatListFragment, false)
        }

        glide.load(args.otherEndUser.profilePicUrl).into(binding.CIVProfilePic)
        binding.toolbar.title = args.otherEndUser.username

        binding.toolbar.setOnClickListener {
            findNavController().navigate(
                ChatFragmentDirections.globalActionToOthersProfileFragment(args.otherEndUser.uid)
            )
        }

        subscribe()

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
                if (it != NO_MORE_MESSAGES) {
                    snackBar(it)
                }
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
        viewModel.isUserOnline.observe(viewLifecycleOwner, EventObserver {
            if (it.state == ONLINE)
                binding.toolbar.subtitle = ONLINE
            else
                binding.toolbar.subtitle =
                    LAST_SEEN + " " + simpleDateFormat.format(Date(it.lastSeen))
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
        viewModel.clearChatList()
//        viewModel.chatList.removeObservers(viewLifecycleOwner)
        super.onDestroy()

        val sp = requireActivity().getSharedPreferences(CHATTING_WITH, Application.MODE_PRIVATE)
        sp.edit().let {
            it.putString(Constants.UID, NO_ONE)
            it.apply()
        }
    }

    companion object {
        const val TAG = "ChatFragment"
    }

}