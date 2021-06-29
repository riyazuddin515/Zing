package com.riyazuddin.zing.ui.main.fragments

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.algolia.search.dsl.ruleQuery
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentProfileBinding
import com.riyazuddin.zing.databinding.ProfileBottomSheetBinding
import com.riyazuddin.zing.other.Constants.PRIVATE
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


    protected open var currentUser: User? = null

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: ProfileViewModel by viewModels()
            return vm
        }

    protected val viewModel: ProfileViewModel
        get() = basePostViewModel as ProfileViewModel

    protected open val uid: String
        get() = FirebaseAuth.getInstance().uid!!

    private lateinit var binding: FragmentProfileBinding

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetBinding: ProfileBottomSheetBinding
    private var sharedPreferences: SharedPreferences? = null

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
        setUpBottomSlider()

        binding.btnToggleFollow.isVisible = false
        binding.btnEditProfile.isVisible = uid == Firebase.auth.uid
        viewModel.loadProfile(uid)
        viewModel.getUserMetaData(uid)
        viewModel.setUid(uid)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        setupClickListeners()

        lifecycleScope.launch {
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
        viewModel.userData.observe(viewLifecycleOwner, EventObserver(
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
            if (user.bio.isNotEmpty()) {
                binding.tvBio.text = user.bio
            }
            if (user.privacy == PRIVATE) {
                bottomSheetBinding.tvFollowingRequests.isVisible = true
                sharedPreferences = requireContext().getSharedPreferences("haveFollowingRequests", MODE_PRIVATE)
                val bool = sharedPreferences?.getBoolean("haveFollowingRequests", false) ?: false
                if (bool) {
                    binding.ivMoreBadge.isVisible = true
                    bottomSheetBinding.tvFollowingRequests.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_circle_primary_color, 0)
//                    bottomSheetBinding.tvFollowingRequests.setOnClickListener {
//                        bottomSheetBinding.tvFollowingRequests.setCompoundDrawablesWithIntrinsicBounds(0,0,0, 0)
//                        sp.edit().let {
//                            it.putBoolean("haveFollowingRequests", false)
//                            it.apply()
//                        }
//                        bottomSheetDialog.dismiss()
//                        findNavController().navigate(R.id.action_profileFragment_to_followersRequestFragment)
//                    }
                }
            }
        })
        viewModel.userMetadata.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
        ) { userMetadata ->
            binding.tvPostCount.text = userMetadata.postCount.toString()
            binding.tvFollowingCount.text = userMetadata.followingCount.toString()
            binding.tvFollowersCount.text = userMetadata.followersCount.toString()
        })

        basePostViewModel.deletePostStatus.observe(viewLifecycleOwner, EventObserver(
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
        binding.ivMore.setOnClickListener {
            binding.ivMoreBadge.isVisible = false
            bottomSheetDialog.show()
        }
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
//            adapter = postAdapterStaggered
//            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = postAdapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setUpBottomSlider() {
        bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        bottomSheetBinding = ProfileBottomSheetBinding.inflate(layoutInflater)
        bottomSheetBinding.apply {
            btnEditProfile.setOnClickListener {
                findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToProfileInfo())
                bottomSheetDialog.dismiss()
            }
            tvFollowingRequests.setOnClickListener {
                bottomSheetBinding.tvFollowingRequests.setCompoundDrawablesWithIntrinsicBounds(0,0,0, 0)
                sharedPreferences?.edit()?.let {
                    it.putBoolean("haveFollowingRequests", false)
                    it.apply()
                }
                bottomSheetDialog.dismiss()
                findNavController().navigate(R.id.action_profileFragment_to_followersRequestFragment)
            }
        }
        bottomSheetDialog.setContentView(bottomSheetBinding.root)
    }

    companion object {
        const val TAG = "ProfileFagLog"
    }
}