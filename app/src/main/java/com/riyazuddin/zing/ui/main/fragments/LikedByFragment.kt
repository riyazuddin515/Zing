package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.databinding.FragmentLikedByBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LikedByFragment : Fragment(R.layout.fragment_liked_by) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: FragmentLikedByBinding

    private val args: LikedByFragmentArgs by navArgs()

    @Inject
    lateinit var userAdapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLikedByBinding.bind(view)

        subscribeToObservers()
        viewModel.getPostLikedUsers(args.postId)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun subscribeToObservers() {
        viewModel.postLikedUsersStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { snackBar(it) },
        ) {
            userAdapter.users = it
            setUpRecyclerView()
        })
    }

    private fun setUpRecyclerView() {
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

}