package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentProfileBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
open class ProfileFragment : BasePostFragment(R.layout.fragment_profile) {

    override val source: String
        get() = (R.id.profileFragment).toString()

    private lateinit var binding: FragmentProfileBinding

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
        viewModel.setUid(uid)
        viewModel.loadProfile(uid)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()

        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest {
                binding.progressBar.isVisible =
                    it.refresh is LoadState.Loading ||
                            it.append is LoadState.Loading
            }
        }

        val bundle = Bundle().apply {
            putString("uid", uid)
        }

        binding.tvFollowersCount.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_followersFragment, bundle)
        }
        binding.tvFollowingCount.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_followingFragment, bundle)
        }
        binding.tvFollowers.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_followersFragment, bundle)
        }
        binding.tvFollowing.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_followingFragment, bundle)
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