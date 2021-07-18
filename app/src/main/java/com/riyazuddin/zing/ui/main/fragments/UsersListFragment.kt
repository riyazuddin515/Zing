package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapterPagingData
import com.riyazuddin.zing.databinding.FragmentUsersBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.NavGraphArgsConstants.FOLLOWERS_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.FOLLOWING_ARG
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.UsersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UsersListFragment : Fragment(R.layout.fragment_users) {

    companion object {
        const val TAG = "UsersListFragment"
    }

    private lateinit var binding: FragmentUsersBinding
    private val viewModel: UsersViewModel by viewModels()
    private val args: UsersListFragmentArgs by navArgs()

    @Inject
    lateinit var userAdapterPagingData: UserAdapterPagingData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentUsersBinding.inflate(layoutInflater)

        setupRecyclerView()
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

        binding.toolbar.title = args.title

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        if (args.title == FOLLOWING_ARG || args.title == FOLLOWERS_ARG) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.flowOfUsersForFollowingAndFollowers(args.id, args.title).collect {
                    userAdapterPagingData.submitData(it)
                }
            }
        }else{
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.flowOfUsersForLikesAndFollowerRequest(args.id, args.title).collect {
                    userAdapterPagingData.submitData(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            userAdapterPagingData.loadStateFlow.collect {
                binding.linearProgressIndicatorFirstLoad.isVisible =
                    it.refresh is LoadState.Loading
                binding.linearProgressIndicatorLoadMore.isVisible =
                    it.append is LoadState.Loading
            }
        }

        userAdapterPagingData.setOnUserClickListener {
            if (Firebase.auth.uid == it.uid)
                findNavController().navigate(R.id.profileFragment)
            else
                findNavController().navigate(
                    UsersListFragmentDirections.globalActionToOthersProfileFragment(it.uid)
                )
        }
    }

    private fun setupRecyclerView() {
        binding.rvUsers.apply {
            adapter = userAdapterPagingData
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
}