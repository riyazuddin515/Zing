package com.riyazuddin.zing.ui.main.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
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
import com.riyazuddin.zing.other.Constants.PRIVATE
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.MainActivity
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
            viewModel.feed.collectLatest {
                postAdapter.submitData(it)
            }
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

    }

    private fun subscribeToObservers() {
        viewModel.loadCurrentUserStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = { Log.i(TAG, "subscribeToObservers: loading current user") }
        ) {
            currentUser = it
            binding.ibRecentChat.isVisible = true
            if (it.privacy == PRIVATE)
                viewModel.checkDoesUserHaveFollowerRequests()
        })
        viewModel.doesUserHaveFollowingRequests.observe(viewLifecycleOwner, EventObserver {
            if (it) {
                Log.i(TAG, "subscribeToObservers: true")
                ((requireActivity()) as MainActivity).binding.bottomNavigation.apply {
                    getOrCreateBadge(R.id.profileFragment).apply {
                        backgroundColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                        isVisible = true
                    }
                }
                val sp = requireContext().getSharedPreferences("haveFollowingRequests", MODE_PRIVATE)
                sp.edit()?.let { editor ->
                    editor.putBoolean("haveFollowingRequests", true)
                    editor.apply()
                }
            } else
                Log.i(TAG, "subscribeToObservers: false")
        })
        viewModel.haveUnSeenMessages.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
            }
        ) {
            if (it) {
                binding.tvHaveUnseenMessages.isVisible = true
                binding.tvHaveUnseenMessages.startAnimation(animation)
            } else
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
        }

        postAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    (binding.rvPostList.layoutManager as LinearLayoutManager).scrollToPosition(0)
                }
            }
        })
    }

    companion object {
        const val TAG = "HomeFragment"
    }

}