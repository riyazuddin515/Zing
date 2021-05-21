package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import com.riyazuddin.zing.ui.main.viewmodels.CommentViewModel
import dagger.hilt.android.AndroidEntryPoint
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

        binding.btnSend.setOnClickListener {
            viewModel.createComment(binding.TIEComment.text.toString(), args.postId)
        }
    }

    private fun subscribeToObservers() {
        viewModel.userProfileStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) }
        ) {
            currentUser = it
            glide.load(it.profilePicUrl).into(binding.CIVProfilePic)
        })

        viewModel.commentsListStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) { comments ->
            binding.progressBar.isVisible = false
            commentAdapter.comments = comments
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
            commentAdapter.comments += comment
            binding.rvComments.smoothScrollToPosition(commentAdapter.itemCount)
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