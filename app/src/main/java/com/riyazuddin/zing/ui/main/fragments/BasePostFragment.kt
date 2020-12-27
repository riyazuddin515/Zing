package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.PostAdapter
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import javax.inject.Inject

abstract class BasePostFragment(layoutId: Int) : Fragment(layoutId) {

    protected abstract val postProgressBar: ProgressBar
    protected abstract val basePostViewModel: BasePostViewModel

    protected abstract val sourceToDestinationLayoutId: Int

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
            basePostViewModel.getUsers(it.likedBy)
        }
    }

    private fun subscribeToObservers() {
        basePostViewModel.likePostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                curLikeIndex?.let { index ->
                    postAdapter.posts[index].isLiking = false
                    postAdapter.notifyItemChanged(index)
                }
                snackBar(it)
            },
            onLoading = {
                curLikeIndex?.let { index ->
                    postAdapter.posts[index].isLiking = true
                    postAdapter.notifyItemChanged(index)
                }
            }
        ) { isLiked ->
            curLikeIndex?.let { index ->
                val uid = FirebaseAuth.getInstance().uid!!
                postAdapter.posts[index].apply {
                    this.isLiked = isLiked
                    this.isLiking = false
                    if (isLiked) {
                        likedBy += uid
                    } else {
                        likedBy -= uid
                    }
                }
                postAdapter.notifyItemChanged(index)
            }
        })

        basePostViewModel.deletePostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) }
        ) { post ->
            postAdapter.posts - post
        })

        basePostViewModel.posts.observe(viewLifecycleOwner, EventObserver(
            onError = {
                postProgressBar.isVisible = false
                snackBar(it)
            },
            onLoading = {
                postProgressBar.isVisible = true
            }
        ) { posts ->
            postAdapter.posts = posts
            postProgressBar.isVisible = false
        })


        basePostViewModel.likedByStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) }
        ) { users ->
            val userAdapter = UserAdapter(glide)
            userAdapter.users = users
            val bundle = Bundle().apply {
                putSerializable("userAdapter", userAdapter)
            }
            findNavController().navigate(sourceToDestinationLayoutId, bundle)
        })
    }
}