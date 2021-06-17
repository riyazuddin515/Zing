package com.riyazuddin.zing.ui.main.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentProfileBinding
import com.riyazuddin.zing.other.EventObserver

class OthersProfileFragment : ProfileFragment() {

    private val args: OthersProfileFragmentArgs by navArgs()
    override val uid: String
        get() = args.uid

    private var curUser: User? = null

    private lateinit var binding: FragmentProfileBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)

        binding.btnToggleFollow.setOnClickListener {
            viewModel.toggleFollowForUser(uid)
        }

        subscribeToObservers()
        setupClickListeners()

    }

    private fun subscribeToObservers() {
        viewModel.loadProfileMetadata.observe(viewLifecycleOwner, EventObserver { user ->
            binding.btnToggleFollow.isVisible = true
            setUpToggleFollowButton(user)
            curUser = user
        })
        viewModel.followStatus.observe(viewLifecycleOwner, EventObserver {
            curUser?.isFollowing = it
            setUpToggleFollowButton(curUser ?: return@EventObserver)
        })
    }

    private fun setupClickListeners() {
        binding.tvFollowersCount.setOnClickListener {
            findNavController().navigate(
                OthersProfileFragmentDirections.globalActionToUserListFragment(uid, "Followers")
            )
        }
        binding.tvFollowingCount.setOnClickListener {
            findNavController().navigate(
                OthersProfileFragmentDirections.globalActionToUserListFragment(uid, "Following")
            )
        }
        binding.tvFollowers.setOnClickListener {
            findNavController().navigate(
                OthersProfileFragmentDirections.globalActionToUserListFragment(uid, "Followers")
            )
        }
        binding.tvFollowing.setOnClickListener {
            findNavController().navigate(
                OthersProfileFragmentDirections.globalActionToUserListFragment(uid, "Following")
            )
        }
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_profileInfo)
        }
        postAdapter.setOnLikedByClickListener {
            findNavController().navigate(
                OthersProfileFragmentDirections.globalActionToUserListFragment(it.postId, "LikedBy")
            )
        }
        postAdapter.setOnCommentClickListener {
            findNavController().navigate(
                OthersProfileFragmentDirections.globalActionToCommentsFragment(it.postId, null)
            )
        }
    }

    private fun setUpToggleFollowButton(user: User) {
        binding.btnToggleFollow.apply {
            val changeBounce = ChangeBounds().apply {
                duration = 300
                interpolator = OvershootInterpolator()
            }
            val set1 = ConstraintSet()
            val set2 = ConstraintSet()
            set1.clone(requireContext(), R.layout.fragment_profile)
            set2.clone(requireContext(), R.layout.fragment_profile_anim)
            TransitionManager.beginDelayedTransition(binding.profileLayout, changeBounce)
            if (user.isFollowing) {
                text = requireContext().getString(R.string.unfollow)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                setTextColor(Color.WHITE)
                set1.applyTo(binding.profileLayout)
            } else {
                text = requireContext().getString(R.string.follow)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
                setTextColor(Color.WHITE)
                set2.applyTo(binding.profileLayout)
            }
        }
    }
}