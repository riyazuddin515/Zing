package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.databinding.FragmentFollowingBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.FollowingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FollowingFragment : Fragment(R.layout.fragment_following) {

    private val viewModel: FollowingViewModel by viewModels()
    private val args: FollowingFragmentArgs by navArgs()

    private lateinit var binding: FragmentFollowingBinding

    @Inject
    lateinit var userAdapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFollowingBinding.bind(view)

        viewModel.getFollowing(args.uid)

        subscribeToObservers()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        userAdapter.setOnUserClickListener {
            if (Firebase.auth.uid == it.uid)
                findNavController().navigate(R.id.profileFragment)
            else
                findNavController().navigate(
                    FollowingFragmentDirections.globalActionToOthersProfileFragment(
                        it.uid
                    )
                )
        }
    }

    private fun subscribeToObservers() {
        viewModel.followingListUsers.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = {
                binding.progressBar.isVisible = false
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) { users ->
            binding.progressBar.isVisible = false
            userAdapter.users = users
            setUpRecyclerView()
        })
    }

    private fun setUpRecyclerView() {
        binding.rvUsers.apply {
            binding.rvUsers.apply {
                adapter = userAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }
}