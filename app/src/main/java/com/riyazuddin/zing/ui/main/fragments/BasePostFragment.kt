package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
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
                getString(R.string.delete_post_dialog_message),
                getString(R.string.delete),
                getString(R.string.cancel)
            ).apply {
                setPositiveListener {
                    basePostViewModel.deletePost(post)
                }
            }.show(childFragmentManager, null)
        }
    }

    private fun subscribeToObservers() {
        basePostViewModel.likePostStatus.observe(viewLifecycleOwner, EventObserver(
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