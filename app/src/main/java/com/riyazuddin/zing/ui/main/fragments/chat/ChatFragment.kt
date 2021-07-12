package com.riyazuddin.zing.ui.main.fragments.chat

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.ChatAdapter
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.databinding.ChatAttachmentLayoutBinding
import com.riyazuddin.zing.databinding.FragmentChatBinding
import com.riyazuddin.zing.other.*
import com.riyazuddin.zing.other.Constants.CHATTING_WITH
import com.riyazuddin.zing.other.Constants.IMAGE
import com.riyazuddin.zing.other.Constants.LAST_SEEN
import com.riyazuddin.zing.other.Constants.NOTIFICATION_ID
import com.riyazuddin.zing.other.Constants.NO_MORE_MESSAGES
import com.riyazuddin.zing.other.Constants.NO_ONE
import com.riyazuddin.zing.other.Constants.ONLINE
import com.riyazuddin.zing.other.NavGraphArgsConstants.ID_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.TIME_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.URL_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.USERNAME_ARG
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
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

    private lateinit var bottomSheetDialogBinding: ChatAttachmentLayoutBinding
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var isFirstBottomSheetSetup: Boolean = true

    private lateinit var resultLauncher: ActivityResultLauncher<String?>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri?>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String?>
    private var cameraUri: Uri? = null

    private val currentUid = Firebase.auth.uid!!

    private var replyToMessageId: String? = null

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var chatAdapter: ChatAdapter

    @Inject
    lateinit var utils: Utils

    @Inject
    lateinit var messageSwipeController: MessageSwipeController

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
        viewModel.getChatLoadFirstQuery(currentUid, args.otherEndUser.uid, args.otherEndUser.name)

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it)
                cameraUri?.let {
                    val bundle = Bundle().apply {
                        putString("stringUri", it.toString())
                        putString("otherEndUserUid", args.otherEndUser.uid)
                    }
                    findNavController().navigate(
                        R.id.action_chatFragment_to_imagePreviewFragment,
                        bundle
                    )
                }
            else
                Toast.makeText(requireContext(), "false", Toast.LENGTH_SHORT).show()
        }
        resultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bundle = Bundle().apply {
                    putString("stringUri", it.toString())
                    putString("otherEndUserUid", args.otherEndUser.uid)
                    putString("replyToMessageId", replyToMessageId)
                }
                findNavController().navigate(
                    R.id.action_chatFragment_to_imagePreviewFragment,
                    bundle
                )
            }
        }
        cameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    cameraUri = getFileUri()
                    cameraLauncher.launch(cameraUri)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences =
            requireActivity().getSharedPreferences(CHATTING_WITH, Application.MODE_PRIVATE)
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
        viewModel.playTone.observe(viewLifecycleOwner, EventObserver {
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
        ) {
            binding.TIEMessage.text?.clear()
            removeRelyToLayout()
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
                replyToMessageId
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
        binding.btnAttachFile.setOnClickListener {
            setupAttachmentSheet()
        }
        chatAdapter.setOnImagePreviewClickListener { imageView, id, url, date ->
            val bundle = Bundle().apply {
                putString(ID_ARG, id)
                putString(USERNAME_ARG, args.otherEndUser.username)
                putString(URL_ARG, url)
                val d = date?.let {
                    simpleDateFormat.format(it)
                }
                putString(TIME_ARG, d)
            }
            val extras = FragmentNavigatorExtras(imageView to id)
            findNavController().navigate(
                R.id.action_chatFragment_to_viewChatMediaFragment,
                bundle,
                null,
                extras
            )
        }
        binding.closeReplyLayout.setOnClickListener {
            removeRelyToLayout()
        }
    }

    private fun removeRelyToLayout() {
        replyToMessageId = null
        binding.replyLayout.isVisible = false
        binding.replyToUsername.text = ""
        binding.replyToMessage.text = ""
    }

    private fun setupAttachmentSheet() {
        if (isFirstBottomSheetSetup) {
            bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
            bottomSheetDialogBinding = ChatAttachmentLayoutBinding.inflate(layoutInflater).apply {
                ivCamera.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    hasCameraPermission()
                }
                ivGallery.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    resultLauncher.launch("image/*")
                }
            }
            bottomSheetDialog.setContentView(bottomSheetDialogBinding.root)
            bottomSheetDialog.show()
            isFirstBottomSheetSetup = false
        } else
            bottomSheetDialog.show()
    }

    private fun setUpRecyclerView() {
        binding.rvChat.apply {
            adapter = chatAdapter
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true
                stackFromEnd = false
            }
            postponeEnterTransition()
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val pos = layoutManager.findLastCompletelyVisibleItemPosition()
                    val numItems: Int = recyclerView.adapter!!.itemCount

                    if (pos + 1 == numItems) {
                        viewModel.getChatLoadMore(
                            currentUid,
                            args.otherEndUser.uid,
                            args.otherEndUser.name
                        )
                    }
                }
            })
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom) {
                    binding.rvChat.smoothScrollToPosition(0)
                }
            }
            messageSwipeController.setOnSwipeControllerAction {
                showQuotedMessage(chatAdapter.messages[it])
            }
            ItemTouchHelper(messageSwipeController).attachToRecyclerView(this)
        }
    }

    private fun showQuotedMessage(message: Message) {
        binding.apply {
            TIEMessage.requestFocus()
            val inputMethodManager =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(TIEMessage, InputMethodManager.SHOW_IMPLICIT)

            replyToUsername.text = if (message.senderAndReceiverUid[0] == currentUid)
                getString(R.string.you)
            else
                args.otherEndUser.name

            if (message.type == IMAGE) {
                glide.load(message.url).into(replyToImage)
                replyToMessage.text = getString(R.string.photo_emoji_with_text)
            } else {
                replyToMessage.text = message.message
            }
            val randomColor = utils.randomPrideColor()
            replyToUsername.setTextColor(randomColor)
            viewLine.setBackgroundColor(randomColor)
            replyToMessageId = message.messageId
            replyLayout.isVisible = true
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

    private fun hasCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) -> {
                cameraUri = getFileUri()
                cameraLauncher.launch(cameraUri)
            }
            else -> {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                else
                    CustomDialog(
                        getString(R.string.permission_needed),
                        getString(R.string.camera_permission_message)
                    ).apply {
                        setPositiveListener {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", requireContext().packageName, null)
                                startActivity(this)
                                dismiss()
                            }
                        }
                    }.show(parentFragmentManager, null)

            }
        }
    }

    private fun getFileUri(): Uri? {
        val file = File.createTempFile(
            System.currentTimeMillis().toString(), ".jpg",
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        return FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", file
        )
    }
}