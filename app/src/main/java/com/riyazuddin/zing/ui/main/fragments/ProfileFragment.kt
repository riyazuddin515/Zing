package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentProfileBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.NavGraphArgsConstants.FOLLOWERS_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.FOLLOWING_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.LIKED_BY_ARG
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
open class ProfileFragment : BasePostFragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

     var currentUser: User? = null

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: ProfileViewModel by viewModels()
            return vm
        }

    protected val viewModel: ProfileViewModel
        get() = basePostViewModel as ProfileViewModel


    protected open val uid: String
        get() = FirebaseAuth.getInstance().uid!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentProfileBinding.inflate(layoutInflater)

        setUpRecyclerView()

        binding.btnToggleFollow.isVisible = false
        binding.btnEditProfile.isVisible = uid == Firebase.auth.uid
        viewModel.setUid(uid)
        viewModel.loadProfile(uid)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        setupClickListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest {
                binding.linearProgressIndicatorFirstLoad.isVisible = it.refresh is LoadState.Loading
                binding.linearProgressIndicatorLoadMore.isVisible = it.append is LoadState.Loading
            }
        }

    }

    private fun subscribeToObservers() {
        viewModel.flowOfProfilePosts.observe(viewLifecycleOwner, {
            postAdapter.submitData(viewLifecycleOwner.lifecycle, it)
        })
        viewModel.loadProfileMetadata.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBarProfileMetadata.isVisible = false
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {
                binding.progressBarProfileMetadata.isVisible = true
            }
        ) { user ->
            currentUser = user
            binding.progressBarProfileMetadata.isVisible = false
            binding.tvName.text = user.name
            binding.toolbar.title = user.username
            glide.load(user.profilePicUrl).into(binding.CIVProfilePic)
            binding.tvBio.text = if (user.bio.isEmpty()) "No Bio" else user.bio

            binding.tvPostCount.text = user.postCount.toString()
            binding.tvFollowingCount.text = user.followingCount.toString()
            binding.tvFollowersCount.text = user.followersCount.toString()
        })

        basePostViewModel.deletePostStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            }
        ) { deletedPost ->
            snackBar("Post Deleted.")
            binding.tvPostCount.text = (binding.tvPostCount.text.toString().toInt() - 1).toString()
            viewModel.removeFromLiveData(deletedPost)
        })
    }

    private fun setupClickListeners() {
        binding.tvFollowersCount.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(uid, FOLLOWERS_ARG)
            )
        }
        binding.tvFollowingCount.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(uid, FOLLOWING_ARG)
            )
        }
        binding.tvFollowers.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(uid, FOLLOWERS_ARG)
            )
        }
        binding.tvFollowing.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(uid, FOLLOWING_ARG)
            )
        }
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_profileInfo)
        }
        postAdapter.setOnLikedByClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(it.postId, LIKED_BY_ARG)
            )
        }
        postAdapter.setOnCommentClickListener { post ->
            currentUser?.let {
                findNavController().navigate(
                    ProfileFragmentDirections.globalActionToCommentsFragment(post.postId, it)
                )
            } ?: snackBar("Please wait")
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
    }

    companion object {
        const val TAG = "ProfileFagLog"
    }
}