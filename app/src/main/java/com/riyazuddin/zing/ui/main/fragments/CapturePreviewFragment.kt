package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCapturePreviewBinding
import com.riyazuddin.zing.other.Constants.IMAGE
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CapturePreviewFragment : Fragment(R.layout.fragment_capture_preview) {

    private lateinit var binding: FragmentCapturePreviewBinding
    private val args: CapturePreviewFragmentArgs by navArgs()

    @Inject
    lateinit var glide: RequestManager

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCapturePreviewBinding.bind(view)

        subscribeToObservers()

        if (args.chatToCamera.isForChat) {
            args.chatToCamera.uri?.let {
                glide.load(it).into(binding.ivCapturePreviewImage)
            }
        }


        binding.btnSend.setOnClickListener {
            Log.e(TAG, "onViewCreated: isForChat = ${args.chatToCamera.isForChat}")
            if (args.chatToCamera.isForChat) {
                Log.e(TAG, "onViewCreated: inside sending ")
                chatViewModel.sendMessage(
                    currentUid = args.chatToCamera.currentUid,
                    receiverUid = args.chatToCamera.receiver,
                    message = binding.TIECaption.text.toString(),
                    type = IMAGE,
                    uri = args.chatToCamera.uri,
                    senderName = args.chatToCamera.senderName,
                    senderUsername = args.chatToCamera.senderUsername,
                    senderProfilePicUrl = args.chatToCamera.senderProfilePicUrl,
                    receiverName = args.chatToCamera.receiverName,
                    receiverUsername = args.chatToCamera.receiverUsername,
                    receiveProfileUrl = args.chatToCamera.receiverProfilePicUrl
                )
                Log.e(TAG, "onViewCreated: sendMessage called")
            }

        }
    }

    private fun subscribeToObservers() {
        chatViewModel.sendMessageStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) },
            onLoading = { snackBar("sending") }
        ) {
            findNavController().popBackStack(R.id.chatFragment, false)
        })
    }

    companion object {
        const val TAG = "PreviewFragmentLog"
    }

}