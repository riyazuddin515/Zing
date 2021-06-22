package com.riyazuddin.zing.ui.main.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentHomeBinding
import com.riyazuddin.zing.other.Constants.CHATS_COLLECTION
import com.riyazuddin.zing.other.Constants.MESSAGES_COLLECTION
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BasePostFragment(R.layout.fragment_home) {

    @Inject
    lateinit var firestore: FirebaseFirestore

    private lateinit var binding: FragmentHomeBinding

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: HomeViewModel by viewModels()
            return vm
        }
    private val viewModel: HomeViewModel
        get() = basePostViewModel as HomeViewModel

    private lateinit var animation: Animation
    private var currentUser: User? = null

    private val currentUserUid: String? = Firebase.auth.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentHomeBinding.inflate(layoutInflater)
        animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce)

        setUpRecyclerView()
        currentUserUid?.let {
            viewModel.onlineOfflineToggle(it)
            viewModel.getCurrentUser(it)
            viewModel.checkForUnSeenMessage(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        setupClickListeners()

        binding.swipeRefreshLayout.setOnRefreshListener {
            postAdapter.refresh()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest {
                binding.linearProgressIndicatorFirstLoad.isVisible = it.refresh is LoadState.Loading
                binding.linearProgressIndicatorLoadMore.isVisible = it.append is LoadState.Loading
            }
        }

        if (currentUser != null) {
            binding.ibRecentChat.isVisible = true
        }

//        binding.send.setOnClickListener {
//            viewLifecycleOwner.lifecycleScope.launch {
//                sendMessage()
//            }
//        }

    }

    private fun subscribeToObservers() {
        viewModel.feed.observe(viewLifecycleOwner, {
            postAdapter.submitData(viewLifecycleOwner.lifecycle, it)
        })
        viewModel.deletePostStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = { snackBar(it) },
            onLoading = { snackBar("Deleting...") }
        ) { deletedPost ->
            viewModel.removePostFromLiveData(deletedPost)
        })
        viewModel.loadCurrentUserStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = { Log.i(TAG, "subscribeToObservers: loading current user") }
        ) {
            currentUser = it
            binding.ibRecentChat.isVisible = true
        })
        viewModel.removeDeviceTokeStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {
                Toast.makeText(requireContext(), "Logging Out", Toast.LENGTH_SHORT).show()
            }
        ) {
            if (it) {
                Firebase.auth.signOut()
                Intent(requireActivity(), AuthActivity::class.java).apply {
                    startActivity(this)
                    requireActivity().finish()
                }
            } else {
                snackBar("can't logout. Try again")
            }
        })
        viewModel.haveUnSeenMessages.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
            }
        ) {
            if (it) {
                binding.tvHaveUnseenMessages.isVisible = true
                binding.tvHaveUnseenMessages.startAnimation(animation)
            }else
                binding.tvHaveUnseenMessages.isVisible = false
        })
    }

    private fun setupClickListeners() {
        postAdapter.setOnUserClickListener {
            if (it.postedBy == currentUserUid)
                findNavController().navigate(R.id.profileFragment)
            else
                findNavController().navigate(
                    HomeFragmentDirections.globalActionToOthersProfileFragment(
                        it.postedBy
                    )
                )
        }

        postAdapter.setOnLikedByClickListener {
            findNavController().navigate(
                HomeFragmentDirections.globalActionToUserListFragment(it.postId, "LikedBy")
            )
        }

        postAdapter.setOnCommentClickListener { post ->
            currentUser?.let {
                findNavController().navigate(
                    HomeFragmentDirections.globalActionToCommentsFragment(post.postId, it)
                )
            } ?: snackBar("Please wait...")
        }
        binding.btnLogout.setOnClickListener {
            CustomDialog("Log Out", " Are you sure to logout of the app?").apply {
                setPositiveListener {
//                    Firebase.auth.signOut()
                    viewModel.removeDeviceToken(currentUserUid!!)
                }
            }.show(parentFragmentManager, null)
        }

        binding.ibRecentChat.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable("currentUser", currentUser)
            }
            findNavController().navigate(R.id.action_homeFragment_to_recentChatListFragment, bundle)
        }
    }

    private fun setUpRecyclerView() {
        binding.rvPostList.apply {
            adapter = postAdapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }

        postAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    (binding.rvPostList.layoutManager as LinearLayoutManager).scrollToPosition(0)
                }
            }
        })
    }

    override fun onDestroy() {
//        viewModel.unSeenMessagesListener?.remove()
        super.onDestroy()
    }
    companion object {
        const val TAG = "HomeFragment"
    }

//    suspend fun sendMessage()  {
//        withContext(Dispatchers.IO){
//
//            val arr = listOf(
//                "fF1FTZ34HoAE952KJCRTGrhL0thX",
//                "6RL1hZFb7u1e8wAPfx5x8jfViVge",
//                "UD4m3tKiKnXarqnY7B46BWA2sRIH",
//                "XDIXCKryjrsiEEIsofcO9QHCcyu9",
//                "QPTCltUK5pIzqdZP8OYEyzNfm71N",
//                "Gw1Ch0DiKr3KZL3EezPgYlY6N9RW",
//                "53kcxSnKKGgZmjiyrNSvwti4LhWX",
//                "Uo5DZAVQZRzn8g5B9puhY31pV9b8",
//                "XNnrEExr6eZr1aCFoXNCfcNSIz6x",
//                "hdXjK5bnbUOwkyJuG4Q42QcrlwaX",
//                "ZJ3JA4NQlOZ8P3QZueg1L7v3yxVC",
//                "EP8aqLMFAc51xlrKAgEWe0Nhdci2",
//                "DAKdCnfJE8rccbPDw0FYEsWt53Qu",
//                "8GNTQTaqOE47nB2B592qnIwJxAfm",
//                "9GdMt56TNQPgF1yYdQSeAzYgT1tx",
//                "AnrllBTPFxrJ55Key7E3fSeb1sff",
//                "HMUrY5xZJuXZgJQIrLlqyZ8OJax9",
//                "GX8ksR7oFk0bWUZmTgq0tVhH2OnB",
//                "0OeYjcSAukYETrQhDePG8XfcOMb2",
//                "Cph7nvfN7k6B13vkSONSRforp0EV",
//                "erZ0PYQpSR9QCOFuw6uI4Zcnaxlq",
//                "p73F16NbqkoZdxKG7CBuu5QHQIwC",
//                "LLTpsAkAUiDn1q0g9kOQjcBdJoel",
//                "1GTVaY85CQNDrHVBl3lWbaGpombw"
//            )
//
//            var i = 1
//            val currentUid = Firebase.auth.uid!!
//            arr.forEach { uid ->
//                val chatThread = getChatThread(currentUid, uid)
//                val messageId = UUID.randomUUID().toString()
//
//                val messageOb = Message(
//                    messageId = messageId,
//                    message = "$i",
//                    date = System.currentTimeMillis(),
//                    senderAndReceiverUid = listOf(uid ,currentUid),
//                    url = ""
//                )
//                firestore.collection(CHATS_COLLECTION)
//                    .document(chatThread).collection(MESSAGES_COLLECTION).document(messageId)
//                    .set(messageOb).await()
//
//                val lastMessage =
//                    LastMessage(message = messageOb, chatThread = chatThread, receiverUid = currentUid)
//                firestore.collection(CHATS_COLLECTION).document(chatThread).set(lastMessage).await()
//                i += 1
//            }
//
//        }
//    }
//    private fun getChatThread(currentUid: String, otherEndUserUid: String) =
//        if (currentUid < otherEndUserUid)
//            currentUid + otherEndUserUid
//        else
//            otherEndUserUid + currentUid

}