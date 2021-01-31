package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.databinding.FragmentProfileBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.PostPreviewDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
open class ProfileFragment : BasePostFragment(R.layout.fragment_profile) {

    override val source: String
        get() = (R.id.profileFragment).toString()

    private lateinit var binding: FragmentProfileBinding

    override val postProgressBar: ProgressBar
        get() = binding.progressBar

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: ProfileViewModel by viewModels()
            return vm
        }

    protected val viewModel: ProfileViewModel
        get() = basePostViewModel as ProfileViewModel


    protected open val uid: String
        get() = FirebaseAuth.getInstance().uid!!

    private lateinit var dialogs: PostPreviewDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)

        setUpGridPostRecyclerView()
        setUpRecyclerView()
        subscribeToObservers()

        binding.btnToggleFollow.isVisible = false
        viewModel.loadProfile(uid)

        binding.btnPostsInGrid.setOnClickListener {
            binding.rvPostGrid.isVisible = true
            binding.rvPostList.isVisible = false
        }

        binding.btnPostsInList.setOnClickListener {
            binding.rvPostList.isVisible = true
            binding.rvPostGrid.isVisible = false
        }

        gridPostAdapter.setItemOnLongListener { post ->

            val dialogLayout = layoutInflater.inflate(R.layout.item_grid_post_preview, null)

            val postImage = dialogLayout.findViewById<ImageView>(R.id.ivPostPreview)
            val authorImage = dialogLayout.findViewById<ImageView>(R.id.CIVProfilePic)
            val userName = dialogLayout.findViewById<MaterialTextView>(R.id.tvUsername)
            userName.text = post.username

            glide.load(post.userProfilePic).into(authorImage)
            glide.load(post.imageUrl).into(postImage)

            dialogs = PostPreviewDialog(dialogLayout)

            showImagePreview()
        }

        binding.rvPostGrid.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_UP)
                    hidePreviewImage()
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun subscribeToObservers() {
        viewModel.loadProfileMetadata.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBarProfileMetadata.isVisible = false
                snackBar(it)
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
        })

        basePostViewModel.deletePostStatus.observe(viewLifecycleOwner, EventObserver {
            gridPostAdapter.posts -= it
        })
    }

    private fun setUpRecyclerView() {
        binding.rvPostList.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

    private fun setUpGridPostRecyclerView() {
        binding.rvPostGrid.apply {
            adapter = gridPostAdapter
            layoutManager = GridLayoutManager(context, 3)
            itemAnimator = null
        }
    }

    private fun showImagePreview() {
        dialogs.show(childFragmentManager, null)
    }

    private fun hidePreviewImage() {
        dialogs.dismiss()
    }
}