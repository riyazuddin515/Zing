package com.riyazuddin.zing.ui.main.fragments.stream_chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.databinding.FragmentChatBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.fragments.BindingFragment
import com.riyazuddin.zing.ui.main.viewmodels.GetStreamViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.ui.message.input.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModel
import io.getstream.chat.android.ui.message.list.header.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.factory.MessageListViewModelFactory
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : BindingFragment<FragmentChatBinding>() {

    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentChatBinding::inflate

    private val args: ChatFragmentArgs by navArgs()
    private val viewModel: GetStreamViewModel by viewModels()

    @Inject
    lateinit var chatClient: ChatClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        Firebase.auth.uid?.let {
            if (chatClient.getCurrentUser() == null) {
                val user = User(
                    id = it,
                )
                viewModel.connectUser(user)
            } else
                bind()
        }
    }

    private fun subscribeToObservers() {
        viewModel.connectUserStatus.observe(viewLifecycleOwner, EventObserver(
            onLoading = {
                Log.i(
                    ChannelFragment.TAG,
                    "subscribeToObservers: connecting user..."
                )
            },
            onError = { snackBar(it) }
        ) {
            bind()
        })
    }

    private fun bind() {
        val factory = MessageListViewModelFactory(args.channelId)
        val messageListHeaderViewModel: MessageListHeaderViewModel by viewModels { factory }
        val messageListViewModel: MessageListViewModel by viewModels { factory }
        val messageInputViewModel: MessageInputViewModel by viewModels { factory }

        messageListHeaderViewModel.bindView(binding.messageListHeaderView, this)
        messageListViewModel.bindView(binding.messageListView, this)
        messageInputViewModel.bindView(binding.messageInputView, this)

        messageListViewModel.mode.observe(viewLifecycleOwner) { mode ->
            when (mode) {
                is MessageListViewModel.Mode.Thread -> {
                    messageListHeaderViewModel.setActiveThread(mode.parentMessage)
                    messageInputViewModel.setActiveThread(mode.parentMessage)
                }
                is MessageListViewModel.Mode.Normal -> {
                    messageListHeaderViewModel.resetThread()
                    messageInputViewModel.resetThread()
                }
            }
        }

        // Step 4 - Let the message input know when we are editing a message
//        binding.messageListView.setMessageEditHandler(messageInputViewModel::postMessageToEdit)

        // Step 5 - Handle navigate up state
        messageListViewModel.state.observe(viewLifecycleOwner) { state ->
            if (state is MessageListViewModel.State.NavigateUp) {
                findNavController().popBackStack()
            }
        }

        // Step 6 - Handle back button behaviour correctly when you're in a thread
        val backHandler = {
            messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
        }
        binding.messageListHeaderView.setBackButtonClickListener(backHandler)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            backHandler()
        }
    }
}