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
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentHomeBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BasePostFragment(R.layout.fragment_home) {

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
            viewModel.getUnSeenLastMessagesCount()
            viewModel.getCurrentUser(it)
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

    }

    private fun subscribeToObservers() {
        viewModel.unSeenLastMessagesCount.observe(viewLifecycleOwner, EventObserver {
            if (it == 0)
                binding.tvUnseenMessageCounter.isVisible = false
            if (it in 1..99) {
                binding.tvUnseenMessageCounter.text = it.toString()
                binding.tvUnseenMessageCounter.isVisible = true
                binding.tvUnseenMessageCounter.startAnimation(animation)
            } else if (it > 99) {
                binding.tvUnseenMessageCounter.text = getString(R.string.plus_99)
                binding.tvUnseenMessageCounter.isVisible = true
            }

        })
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

    companion object {
        const val TAG = "HomeFragment"
    }

}