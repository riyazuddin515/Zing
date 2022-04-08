package com.riyazuddin.zing.ui.main.fragments

import android.app.NotificationManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
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
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentHomeBinding
import com.riyazuddin.zing.other.Constants.NO_USER_DOCUMENT
import com.riyazuddin.zing.other.Constants.PRIVATE
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.NavGraphArgsConstants.CURRENT_USER_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.LIKED_BY_ARG
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.MainActivity
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.GetStreamViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.ChatClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : BasePostFragment(R.layout.fragment_home) {

    companion object {
        const val TAG = "HomeFragmentLog"
    }

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

    private var customDialog: CustomDialog? = null

    @Inject
    lateinit var chatClient: ChatClient
    private val getStreamViewModel by viewModels<GetStreamViewModel>()

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
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearNotifications()
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

        if (currentUser == null) {
            viewModel.getCurrentUser(currentUserUid ?: return)
        }

    }

    private fun updateHaveUnSeenMessages(count: Int) {
        if (count > 0) {
            val a = if (count in 1..99) count.toString() else "99+"
            binding.tvHaveUnseenMessages.text = a
            binding.tvHaveUnseenMessages.isVisible = true
        } else
            binding.tvHaveUnseenMessages.isVisible = false
        binding.tvHaveUnseenMessages.startAnimation(animation)
    }

    private fun subscribeToObservers() {
        viewModel.feedPagingFlow.observe(viewLifecycleOwner) {
            postAdapter.submitData(viewLifecycleOwner.lifecycle, it)
        }
        viewModel.loadCurrentUserStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                if (it == NO_USER_DOCUMENT) {
                    showAccountSetUpDialog()
                } else {
                    snackBar(it)
                    Log.e(TAG, "subscribeToObservers: $it")
                }
            },
            onLoading = { Log.i(TAG, "subscribeToObservers: loading current user") }
        ) {
            currentUser = it
            if (it.privacy == PRIVATE)
                viewModel.checkDoesUserHaveFollowerRequests()
            customDialog?.dismiss()

            val user = io.getstream.chat.android.client.models.User(
                id = it.uid,
                extraData = mutableMapOf(
                    "name" to it.name,
                    "image" to it.profilePicUrl,
                )
            )
            getStreamViewModel.connectUser(user)
        })
        getStreamViewModel.connectUserStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            }
        ) {
            binding.ibRecentChat.isVisible = true
            updateHaveUnSeenMessages(it.unreadChannels)
            Log.i(TAG, "subscribeToObservers: stream connection succeeded")
        })
        viewModel.doesUserHaveFollowingRequests.observe(viewLifecycleOwner, EventObserver {
            if (it) {
                Log.i(TAG, "subscribeToObservers: true")
                ((requireActivity()) as MainActivity).binding.bottomNavigation.apply {
                    getOrCreateBadge(R.id.profileFragment).apply {
                        backgroundColor =
                            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                        isVisible = true
                    }
                }
                val sp =
                    requireContext().getSharedPreferences("haveFollowingRequests", MODE_PRIVATE)
                sp.edit()?.let { editor ->
                    editor.putBoolean("haveFollowingRequests", true)
                    editor.apply()
                }
            }
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
                HomeFragmentDirections.globalActionToUserListFragment(it.postId, LIKED_BY_ARG)
            )
        }

        postAdapter.setOnCommentClickListener { post ->
            currentUser?.let {
                findNavController().navigate(
                    HomeFragmentDirections.globalActionToCommentsFragment(post.postId, it)
                )
            } ?: snackBar(getString(R.string.please_wait))
        }

        binding.ibRecentChat.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable(CURRENT_USER_ARG, currentUser)
            }
            updateHaveUnSeenMessages(0)
            findNavController().navigate(R.id.action_homeFragment_to_channelFragment, bundle)
        }
    }

    private fun setUpRecyclerView() {
        binding.rvPostList.apply {
            adapter = postAdapter
            itemAnimator = null
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            layoutManager = LinearLayoutManager(requireContext())
        }

        postAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    (binding.rvPostList.layoutManager as LinearLayoutManager).scrollToPosition(0)
                }
            }
        })
    }

    private fun clearNotifications() {
        //Removing all existing notification as soon as activity starts
        val notificationManager: NotificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    override fun onResume() {
        super.onResume()
        clearNotifications()
        if (currentUser == null) {
            viewModel.getCurrentUser(currentUserUid ?: return)
        }
    }

    private fun showAccountSetUpDialog() {
        customDialog = CustomDialog(
            getString(R.string.setup_your_account),
            getString(R.string.setup_your_account_message),
            getString(R.string.setup),
            "",
            false
        ).apply {
            setPositiveListener {
                currentUser = null
                findNavController().navigate(R.id.action_homeFragment_to_profileInfo)
            }
        }
        customDialog?.show(childFragmentManager, null)
    }

}