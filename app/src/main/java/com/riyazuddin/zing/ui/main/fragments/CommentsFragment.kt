package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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


    companion object {
        const val TAG = "CommentsFragment"
    }

    private var currentUser: User? = null
    private val args: CommentsFragmentArgs by navArgs()
    private val viewModel: CommentViewModel by viewModels()
    private lateinit var binding: FragmentCommentsBinding

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentCommentsBinding.inflate(layoutInflater)

        setUpRecyclerView()
        viewModel.getComments(args.postId)
        args.currentUser?.let {
            currentUser = it
        } ?: viewModel.getUserProfile()
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

        args.currentUser?.let {
            glide.load(it.profilePicUrl).into(binding.CIVProfilePic)
        }

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
                binding.linearProgressIndicatorFirstLoad.isVisible = it.refresh is LoadState.Loading
                binding.linearProgressIndicatorLoadMore.isVisible = it.append is LoadState.Loading
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
            currentUser?.let {
                viewModel.createComment(binding.TIEComment.text.toString(), args.postId)
            } ?: snackBar("Please wait...")
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
            comment.username = currentUser!!.username
            comment.userProfilePic = currentUser!!.profilePicUrl

            viewModel.insertCommentInLiveData(comment)
        })

        viewModel.deleteCommentStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
            },
            onLoading = {
                snackBar("Deleting...")
            }
        ) {
            snackBar("Comment Deleted!")
            viewModel.deleteCommentInLiveData(it)
        })
    }

    private fun setUpRecyclerView() {
        binding.rvComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null

            commentAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (positionStart == 0) {
                        (binding.rvComments.layoutManager as LinearLayoutManager).scrollToPosition(0)
                    }
                }
            })
        }
    }
}