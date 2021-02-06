package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.riyazuddin.zing.data.ChatToCamera
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentChatBinding
import com.riyazuddin.zing.other.Constants.IMAGE
import com.riyazuddin.zing.other.Constants.TEXT
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var currentUser: User? = null

    private lateinit var binding: FragmentChatBinding
    private val viewModel: ChatViewModel by viewModels()

    private val args: ChatFragmentArgs by navArgs()

    @Inject
    lateinit var glide: RequestManager

    private var firestoreChatAdapter: ChatAdapter? = null

    companion object {
        const val TAG = "ChatFragmentLog"
    }

    private var isAttachmentLayoutVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        viewModel.getChat(Firebase.auth.uid!!, args.chatFragmentData.uid)

        binding.tvUsername.text = args.chatFragmentData.username
        glide.load(args.chatFragmentData.profilePicUrl).into(binding.CIVProfilePic)
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        subscribeToObservers()

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSend.setOnClickListener {

            currentUser?.let {
                viewModel.sendMessage(
                    Firebase.auth.uid!!,
                    args.chatFragmentData.uid,
                    binding.TIEMessage.text.toString(),
                    TEXT,
                    senderName = it.name,
                    senderUsername = it.username,
                    senderProfilePicUrl = it.profilePicUrl,
                    receiverName = args.chatFragmentData.name,
                    receiverUsername = args.chatFragmentData.username,
                    receiveProfileUrl = args.chatFragmentData.profilePicUrl,
                )
            }

        }

        binding.btnAttachFile.setOnClickListener {
            showOrHideAttachmentLayout()
        }

        binding.ivCamera.setOnClickListener {
            showOrHideAttachmentLayout()
            currentUser?.let {
                val chatToCamera = ChatToCamera(
                    true,
                    Firebase.auth.uid!!,
                    args.chatFragmentData.uid,
                    IMAGE,
                    senderName = it.name,
                    senderProfilePicUrl = it.profilePicUrl,
                    senderUsername = it.username,
                    receiverName = args.chatFragmentData.name,
                    receiverUsername = args.chatFragmentData.username,
                    receiverProfilePicUrl = args.chatFragmentData.profilePicUrl
                )
                findNavController().navigate(
                    ChatFragmentDirections.globalActionToCameraFragment(chatToCamera)
                )
            }
        }
    }

    private fun showOrHideAttachmentLayout() {
        binding.attachmentLayout.isVisible = !isAttachmentLayoutVisible
        isAttachmentLayoutVisible = !isAttachmentLayoutVisible
    }

    private fun subscribeToObservers() {
        viewModel.chatOptions.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) },
            onLoading = { Log.e(TAG, "subscribeToObservers: onLoading") }
        ) {
            firestoreChatAdapter = ChatAdapter(glide, it)
            firestoreChatAdapter?.startListening()
            setupRecyclerView()
        })

        viewModel.currentUserProfileStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) }
        ) {
            currentUser = it
        })

        viewModel.sendMessageStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
            },
        ) {
            binding.TIEMessage.text?.clear()
        })

        viewModel.deleteMessageStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) }
        ) { message ->
            Log.d(TAG, "subscribeToObservers: message ${message.message} deleted")
        })
    }

    private fun setupRecyclerView() {
        binding.rvChat.apply {

            firestoreChatAdapter?.registerAdapterDataObserver(object :
                RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    binding.rvChat.smoothScrollToPosition(firestoreChatAdapter!!.itemCount)
                }
            })
            firestoreChatAdapter?.setOnItemLongClickListener { message ->
                CustomDialog("Delete Message", "Are you sure?")
                    .apply {
                        setPositiveListener {
                            viewModel.deleteChatMessage(
                                Firebase.auth.uid!!,
                                args.chatFragmentData.uid,
                                message
                            )
                        }
                    }
                    .show(childFragmentManager, null)
            }

            adapter = firestoreChatAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom) {
                    binding.rvChat.smoothScrollToPosition(firestoreChatAdapter!!.itemCount)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        firestoreChatAdapter?.stopListening()
    }

}