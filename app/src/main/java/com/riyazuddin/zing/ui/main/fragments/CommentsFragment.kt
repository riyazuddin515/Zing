package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.CommentAdapter
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentCommentsBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.CommentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CommentsFragment : Fragment(R.layout.fragment_comments) {

    private lateinit var currentUser: User

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var commentAdapter: CommentAdapter
    private val args: CommentsFragmentArgs by navArgs()
    private val viewModel: CommentViewModel by viewModels()
    private lateinit var binding: FragmentCommentsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCommentsBinding.bind(view)

        setUpRecyclerView()
        subscribeToObservers()

        viewModel.getUserProfile()
        viewModel.getComments(args.postId)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            commentAdapter.refresh()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        commentAdapter.setOnUserClickListener {
            if (Firebase.auth.uid == it.commentedBy)
                findNavController().navigate(R.id.profileFragment)
            else
                findNavController().navigate(
                    CommentsFragmentDirections.globalActionToOthersProfileFragment(
                        it.commentedBy
                    )
                )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            commentAdapter.loadStateFlow.collectLatest {
                binding.progressBar.isVisible =
                    it.refresh is LoadState.Loading ||
                            it.append is LoadState.Loading
            }
        }

        commentAdapter.setOnCommentDeleteClickListener {
            CustomDialog(
                getString(R.string.delete_comment_dialog_title),
                getString(R.string.delete_comment_dialog_message)
            ).apply {
                setPositiveListener {
                    viewModel.deleteComment(it)
                }
            }.show(childFragmentManager, null)
        }

        binding.btnSend.setOnClickListener {
            viewModel.createComment(binding.TIEComment.text.toString(), args.postId)
        }
    }

    private fun subscribeToObservers() {
        viewModel.postComments.observe(viewLifecycleOwner, {
            commentAdapter.submitData(viewLifecycleOwner.lifecycle, it)
        })

        viewModel.userProfileStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) }
        ) {
            currentUser = it
            glide.load(it.profilePicUrl).into(binding.CIVProfilePic)
        })

        viewModel.createCommentStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = {
                binding.btnSend.isEnabled = true
                snackBar(it)
            },
            onLoading = { binding.btnSend.isEnabled = false }
        ) { comment ->
            binding.TIEComment.text?.clear()
            binding.btnSend.isEnabled = true
            snackBar("Comment Posted")
            comment.username = currentUser.username
            comment.userProfilePic = currentUser.profilePicUrl

            viewModel.insertCommentInLiveData(comment)
        })

        viewModel.deleteCommentStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
            },
            onLoading = {
                snackBar("Deleting...")
            }
        ){
            snackBar("Comment Deleted!")
            viewModel.deleteCommentInLiveData(it)
        })
    }

    private fun setUpRecyclerView() {
        binding.rvComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

    companion object {
        const val TAG = "CommentsFrag"
    }
}