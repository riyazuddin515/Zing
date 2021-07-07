package com.riyazuddin.zing.ui.main.fragments.chat

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    companion object {
        const val TAG = "ChatFragment"
    }

    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()

    private lateinit var binding: FragmentChatBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var simpleDateFormat: SimpleDateFormat
    private lateinit var sharedPreferences: SharedPreferences

    private val currentUid = Firebase.auth.uid!!

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
        viewModel.getChatLoadFirstQuery(currentUid, args.otherEndUser.uid)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences(CHATTING_WITH, Application.MODE_PRIVATE)
        simpleDateFormat = SimpleDateFormat("d MMM yy hh:mm a", Locale.US)
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.received_chat)

        setupClickListener()
        subscribeToObservers()
        clearNotifications()
        setSharedPreferences(args.otherEndUser.uid)

        glide.load(args.otherEndUser.profilePicUrl).into(binding.CIVProfilePic)
        binding.toolbar.title = args.otherEndUser.username

    }

    private fun subscribeToObservers() {
        viewModel.chatList.observe(viewLifecycleOwner, EventObserver(
            onError = {
                if (it != NO_MORE_MESSAGES)
                    snackBar(it)
                binding.linearProgressIndicator.isVisible = false
            },
            onLoading = {
                binding.linearProgressIndicator.isVisible = true
            }
        ) {
            binding.linearProgressIndicator.isVisible = false
            chatAdapter.messages = it
            chatAdapter.notifyDataSetChanged()
        })
        viewModel.playTone.observe(viewLifecycleOwner, EventObserver(
        ) {
            if (it)
                mediaPlayer.start()
            else
                mediaPlayer.stop()
        })
        viewModel.isUserOnline.observe(viewLifecycleOwner, EventObserver {
            if (it.state == ONLINE)
                binding.toolbar.subtitle = ONLINE
            else
                binding.toolbar.subtitle =
                    LAST_SEEN + " " + simpleDateFormat.format(it.lastSeen!!)
        })
        viewModel.sendMessageStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
            }
        ){
            binding.TIEMessage.text?.clear()
        })
    }

    private fun setupClickListener() {
        binding.ivBack.setOnClickListener {
            hideKeyboard(it)
            findNavController().popBackStack()
        }
        binding.toolbar.setOnClickListener {
            hideKeyboard(it)
            findNavController().navigate(
                ChatFragmentDirections.globalActionToOthersProfileFragment(
                    args.otherEndUser.uid
                )
            )
        }
        binding.btnSend.setOnClickListener {
            viewModel.sendMessage(
                currentUid = currentUid,
                receiverUid = args.otherEndUser.uid,
                message = binding.TIEMessage.text.toString(),
                type = Constants.TEXT,
                uri = null,
            )
        }
        chatAdapter.setOnItemLongClickListener { message ->
            CustomDialog(
                getString(R.string.delete_this_message),
                getString(R.string.are_you_sure_you_want_to_delete_this_message)
            ).apply {
                setPositiveListener {
                    viewModel.deleteMessage(currentUid, args.otherEndUser.uid, message)
                }
            }.show(parentFragmentManager, null)
        }
    }

    private fun setUpRecyclerView() {
        binding.rvChat.apply {
            adapter = chatAdapter
            itemAnimator = null
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
                        viewModel.getChatLoadMore(currentUid, args.otherEndUser.uid)
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

    override fun onDetach() {
        chatAdapter.messages = listOf()
        viewModel.clearChatList()
        viewModel.removeCheckOnlineListener()
        setSharedPreferences(NO_ONE)
        super.onDetach()
    }

    private fun hideKeyboard(view: View) {
        val manager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        manager?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onPause() {
        setSharedPreferences(NO_ONE)
        super.onPause()
    }

    override fun onResume() {
        setSharedPreferences(args.otherEndUser.uid)
        clearNotifications()
        super.onResume()
    }

    private fun setSharedPreferences(id: String) {
        sharedPreferences.edit().let {
            it.putString(Constants.UID, id)
            it.apply()
        }
    }

    private fun clearNotifications() {
        //Removing all existing notification as soon as activity starts
        val notificationManager: NotificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(args.otherEndUser.uid, NOTIFICATION_ID)
    }
}