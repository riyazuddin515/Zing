package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.FollowerRequestsAdapter
import com.riyazuddin.zing.databinding.FragmentFollowersRequestBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.FollowerRequestsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FollowersRequestFragment : Fragment(R.layout.fragment_followers_request) {

    companion object {
        const val TAG = "FollowerRequestFragment"
    }

    private val viewModel: FollowerRequestsViewModel by viewModels()
    private lateinit var binding: FragmentFollowersRequestBinding

    @Inject
    lateinit var followerRequestsAdapter: FollowerRequestsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentFollowersRequestBinding.inflate(layoutInflater)

        binding.rvFollowersRequests.adapter = followerRequestsAdapter

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

        viewLifecycleOwner.lifecycleScope.launch {
            followerRequestsAdapter.loadStateFlow.collect {
                binding.linearProgressIndicatorFirstLoad.isVisible = it.refresh is LoadState.Loading
                binding.linearProgressIndicatorLoadMore.isVisible = it.append is LoadState.Loading
            }
        }

        setupClickListeners()
    }

    private fun subscribeToObservers() {
        viewModel.followerRequests.observe(viewLifecycleOwner, {
            followerRequestsAdapter.submitData(viewLifecycleOwner.lifecycle, it)
        })
        viewModel.actionStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {

            }
        ) {
            viewModel.removeRequestFromLiveData(it)
        })
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        followerRequestsAdapter.setOnClickListener {
            findNavController().navigate(
                FollowersRequestFragmentDirections.globalActionToOthersProfileFragment(it)
            )
        }
        followerRequestsAdapter.setOnActionListener { uid, action ->
            viewModel.acceptOrRejectTheFollowerRequest(uid, action)
        }
    }
}