package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.PostAdapter
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.databinding.FragmentPostBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.Utils.Companion.getTimeAgo
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import com.riyazuddin.zing.ui.main.viewmodels.PostViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PostFragment : Fragment(R.layout.fragment_post) {

    private lateinit var binding: FragmentPostBinding
    private val viewModel: PostViewModel by viewModels()
    private val args: PostFragmentArgs by navArgs()
    private val homeViewModel: HomeViewModel by viewModels()

    private var post: Post? = null

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentPostBinding.inflate(layoutInflater)
        viewModel.getPost(args.postId)
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


        binding.postLayout.ibLike.setOnClickListener {
            post?.let {
                homeViewModel.toggleLikeForPost(it)
            }
        }
        binding.postLayout.ivPostImage.setOnClickListener(object :
            PostAdapter.DoubleClickListener() {
            override fun onDoubleClick(v: View?) {
                post?.let {
                    homeViewModel.toggleLikeForPost(it)
                }
            }
        })
        binding.postLayout.ibComment.setOnClickListener {
            post?.let {
                findNavController().navigate(
                    PostFragmentDirections.globalActionToCommentsFragment(it.postId, null)
                )
            }
        }
        binding.postLayout.tvLikeCount.setOnClickListener {
            post?.let {
                findNavController().navigate(
                    PostFragmentDirections.globalActionToUserListFragment(it.postId, "LikedBy")
                )
            }
        }
        binding.postLayout.ibDelete.setOnClickListener {
            post?.let {
                CustomDialog(
                    getString(R.string.delete_post_dialog_title),
                    getString(R.string.delete_post_dialog_message)
                ).apply {
                    setPositiveListener {
                        homeViewModel.deletePost(it)
                    }
                }.show(childFragmentManager, null)
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.post.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = true
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) { post ->
            binding.progressBar.isVisible = true
            this.post = post
            binding.postLayout.apply {
                glide.load(post.userProfilePic).into(CIVProfilePic)
                glide.load(post.imageUrl).into(ivPostImage)
                tvUsername.text = post.username
                val likeCount = post.likeCount
                tvLikeCount.isVisible = likeCount != 0
                val likesText =
                    if (likeCount == 1) "1 like" else "${String.format("%,d", likeCount)} likes"
                tvLikeCount.text = likesText
                tvPostedOn.text = getTimeAgo(post.date!!.time)
                if (post.caption.isEmpty())
                    tvCaption.isVisible = false
                else tvCaption.text = post.caption
                ibDelete.isVisible = (post.postedBy == Firebase.auth.uid!!)
                ibLike.setImageResource(
                    if (post.isLiked) R.drawable.ic_like_red
                    else R.drawable.ic_outline_like
                )
            }
        })

        homeViewModel.deletePostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {
                snackBar("Deleting...")
            }
        ) {
            snackBar("Post Deleted.")
            findNavController().popBackStack()
        })

        homeViewModel.likePostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                post?.isLiking = false
                snackBar(it)
            },
            onLoading = {
                post?.isLiking = true
            }
        ) { isLiked ->
            post?.isLiked = isLiked
            post?.isLiking = false

            if (isLiked)
                post?.let {
                    it.likeCount = it.likeCount.plus(1)
                }
            else
                post?.let {
                    it.likeCount = it.likeCount.minus(1)
                }
            update()
        })
    }

    private fun update() {
        binding.postLayout.ibLike.setImageResource(
            if (post!!.isLiked) R.drawable.ic_like_red
            else R.drawable.ic_outline_like
        )
        val likeCount = post!!.likeCount
        binding.postLayout.tvLikeCount.isVisible = likeCount != 0
        val likesText =
            if (likeCount == 1) "1 like" else "${String.format("%,d", likeCount)} likes"
        binding.postLayout.tvLikeCount.text = likesText
    }

    companion object {
        const val TAG = "PostFragment"
    }
}