package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.PostAdapter
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import javax.inject.Inject
import kotlin.properties.Delegates

abstract class BasePostFragment(layoutId: Int) : Fragment(layoutId) {

    protected abstract val basePostViewModel: BasePostViewModel

    protected abstract val source: String
    private var sourceToDestinationLayoutId by Delegates.notNull<Int>()

    @Inject
    lateinit var postAdapter: PostAdapter

    @Inject
    lateinit var glide: RequestManager

    private var curLikeIndex: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()

        postAdapter.setOnLikeClickListener { post, i ->
            curLikeIndex = i
            post.isLiked = !post.isLiked
            basePostViewModel.toggleLikeForPost(post)
        }

        postAdapter.setOnDeleteClickListener { post ->
            CustomDialog(
                getString(R.string.delete_post_dialog_title),
                getString(R.string.delete_post_dialog_message)
            ).apply {
                setPositiveListener {
                    basePostViewModel.deletePost(post)
                }
            }.show(childFragmentManager, null)
        }

        postAdapter.setOnLikedByClickListener {
            basePostViewModel.getPostLikedUsers(it.postId)
            when (source) {
                (R.id.homeFragment).toString() -> sourceToDestinationLayoutId =
                    R.id.action_homeFragment_to_likedByFragment
                (R.id.profileFragment).toString() -> sourceToDestinationLayoutId =
                    R.id.action_profileFragment_to_likedByFragment
                (R.id.othersProfileFragment).toString() -> sourceToDestinationLayoutId =
                    R.id.action_othersProfileFragment_to_likedByFragment
            }
            val bundle = Bundle().apply {
                putString("postId", it.postId)
            }
            findNavController().navigate(sourceToDestinationLayoutId, bundle)
        }

        postAdapter.setOnCommentClickListener {
            when (source) {
                (R.id.homeFragment).toString() -> sourceToDestinationLayoutId =
                    R.id.action_homeFragment_to_commentsFragment
                (R.id.profileFragment).toString() -> sourceToDestinationLayoutId =
                    R.id.action_profileFragment_to_commentsFragment
                (R.id.othersProfileFragment).toString() -> sourceToDestinationLayoutId =
                    R.id.action_othersProfileFragment_to_commentsFragment
            }
            val bundle = Bundle().apply {
                putString("postId", it.postId)
            }
            findNavController().navigate(sourceToDestinationLayoutId, bundle)
        }
    }

    private fun subscribeToObservers() {
        basePostViewModel.likePostStatus.observe(viewLifecycleOwner, EventObserver(
            forIsLikedBy = true,
            onError = {
                curLikeIndex?.let { index ->
                    postAdapter.peek(index)?.isLiking = false
                    postAdapter.notifyItemChanged(index)
                }
                snackBar(it)
            },
            onLoading = {
                curLikeIndex?.let { index ->
                    postAdapter.peek(index)?.isLiking = true
                    postAdapter.notifyItemChanged(index)
                }
            }
        ) { isLiked ->
            curLikeIndex?.let { index ->
                postAdapter.peek(index)?.apply {
                    this.isLiked = isLiked
                    this.isLiking = false
                    if (isLiked) {
                        this.likeCount++
                    } else {
                        this.likeCount--
                    }
                }
                postAdapter.notifyItemChanged(index)
            }
        })
    }
}