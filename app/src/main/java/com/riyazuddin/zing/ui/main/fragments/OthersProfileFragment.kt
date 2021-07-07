package com.riyazuddin.zing.ui.main.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentOthersProfileBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.PRIVATE
import com.riyazuddin.zing.other.Constants.PUBLIC
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.NavGraphArgsConstants
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OthersProfileFragment : BasePostFragment(R.layout.fragment_others_profile) {

    companion object {
        const val TAG = "OthersProfileTest"
    }

    private lateinit var binding: FragmentOthersProfileBinding
    private val args: OthersProfileFragmentArgs by navArgs()

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: ProfileViewModel by viewModels()
            return vm
        }

    private val viewModel: ProfileViewModel
        get() = basePostViewModel as ProfileViewModel

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentOthersProfileBinding.inflate(layoutInflater)
        setUpRecyclerView()
        viewModel.loadProfile(args.uid)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
        setClickListeners()
    }

    private fun subscribeToObservers() {
        viewModel.userData.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            }, onLoading = {

            }
        ) { user ->
            this.user = user
            binding.progressBarProfileMetadata.isVisible = false
            binding.tvName.text = user.name
            binding.toolbar.title = user.username
            glide.load(user.profilePicUrl).into(binding.CIVProfilePic)
            if (user.bio.isNotEmpty()) {
                binding.tvBio.text = user.bio
            }

            if (user.privacy == PUBLIC) {
                binding.btnToggleFollow.isVisible = true
                setUpToggleFollowButton(user)
                viewModel.getUserMetaData(user.uid)
                getFlow()
            }
            if (user.privacy == PRIVATE) {
                if (user.isFollowing) {
                    binding.btnToggleFollow.isVisible = true
                    setUpToggleFollowButton(user)
                    viewModel.getUserMetaData(user.uid)
                    getFlow()
                } else {
                    binding.tvPostCount.text = "-"
                    binding.tvFollowingCount.text = "-"
                    binding.tvFollowersCount.text = "-"
                    viewModel.checkForPreviousFollowerRequests(args.uid)
                }
            }
        })
        viewModel.previousFollowingRequests.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            }
        ) {
            setupSendFollowingRequestButton(it)
        })
        viewModel.userMetadata.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(ProfileFragment.TAG, "subscribeToObservers: $it")
            },
        ) { userMetadata ->
            binding.tvPostCount.text = userMetadata.postCount.toString()
            binding.tvFollowingCount.text = userMetadata.followingCount.toString()
            binding.tvFollowersCount.text = userMetadata.followersCount.toString()
        })
        viewModel.followStatus.observe(viewLifecycleOwner, EventObserver {
            if (!it) {
                user?.let { user ->
                    if (user.privacy == PRIVATE) {
                        binding.btnToggleFollow.isVisible = false
                        binding.btnSendFollowRequest.isVisible = true
                        binding.tvPrivateAccountInfo.isVisible = true
                        viewLifecycleOwner.lifecycleScope.launch {
                            postAdapter.submitData(PagingData.empty())
                        }
                        binding.rvPostList.visibility = View.GONE
                    }
                }
            }
            user?.let { user ->
                user.isFollowing = it
                setUpToggleFollowButton(user)
            }
        })

        viewModel.toggleSendFollowingRequest.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            }
        ) {
            setupSendFollowingRequestButton(it)
        })
    }

    private fun setUpRecyclerView() {
        binding.rvPostList.apply {
            adapter = postAdapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun getFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            user?.uid?.let {
                viewModel.otherProfileFeed(it).collect { pagingData ->
                    postAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun setClickListeners() {
        binding.btnSendFollowRequest.setOnClickListener {
            viewModel.toggleSendFollowerRequest(args.uid)
        }
        binding.btnToggleFollow.setOnClickListener {
            user?.let {
                if (it.privacy == PUBLIC) {
                    viewModel.toggleFollowForUser(args.uid)
                } else if (it.privacy == PRIVATE && it.isFollowing) {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.MaterialAlertDialog_Round
                    ).apply {
                        setIcon(R.drawable.ic_warning)
                        setTitle("Warning")
                        setMessage("Note: This account is private. If you UnFollow now, you will have to send a request again to Follow")
                        setPositiveButton("UnFollow") { dialogInterface, _ ->
                            dialogInterface.dismiss()
                            viewModel.toggleFollowForUser(args.uid)
                        }
                        setNegativeButton("Cancel") { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                    }.show()
                }
            }
        }
        binding.tvFollowersCount.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(args.uid,
                    NavGraphArgsConstants.FOLLOWERS_ARG
                )
            )
        }
        binding.tvFollowingCount.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(args.uid,
                    NavGraphArgsConstants.FOLLOWING_ARG
                )
            )
        }
        binding.tvFollowers.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(args.uid,
                    NavGraphArgsConstants.FOLLOWERS_ARG
                )
            )
        }
        binding.tvFollowing.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(args.uid,
                    NavGraphArgsConstants.FOLLOWING_ARG
                )
            )
        }
        postAdapter.setOnLikedByClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.globalActionToUserListFragment(it.postId,
                    NavGraphArgsConstants.LIKED_BY_ARG
                )
            )
        }
        postAdapter.setOnCommentClickListener { post ->
            user?.let {
                findNavController().navigate(
                    ProfileFragmentDirections.globalActionToCommentsFragment(post.postId, it)
                )
            } ?: snackBar("Please wait")
        }
    }

    private fun setUpToggleFollowButton(user: User) {
        binding.btnToggleFollow.apply {
            if (user.isFollowing) {
                text = requireContext().getString(R.string.unfollow)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                setTextColor(Color.WHITE)
            } else {
                text = requireContext().getString(R.string.follow)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
                setTextColor(Color.WHITE)
            }
        }
    }

    private fun setupSendFollowingRequestButton(bool: Boolean) {
        binding.btnSendFollowRequest.apply {
            if (bool) {
                text = context.getString(R.string.undo_request)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                strokeColor = null
                strokeWidth = 0
            } else {
                text = context.getString(R.string.send_request)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                setStrokeColorResource(R.color.black)
                strokeWidth = 2
            }
            visibility = View.VISIBLE
        }
        binding.tvPrivateAccountInfo.isVisible = true
    }

}