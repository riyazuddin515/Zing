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
import com.riyazuddin.zing.databinding.FragmentFollowersBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.FollowersViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FollowersFragment : Fragment(R.layout.fragment_followers) {

    private val viewModel: FollowersViewModel by viewModels()

    private val args: FollowersFragmentArgs by navArgs()
    private lateinit var binding: FragmentFollowersBinding

    @Inject
    lateinit var userAdapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFollowersBinding.bind(view)
        viewModel.getFollowers(uid = args.uid)

        subscribeToObservers()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        userAdapter.setOnUserClickListener {
            if (Firebase.auth.uid == it.uid)
                findNavController().navigate(R.id.profileFragment)
            else
                findNavController().navigate(
                    FollowersFragmentDirections.globalActionToOthersProfileFragment(
                        it.uid
                    )
                )
        }
    }

    private fun subscribeToObservers() {
        viewModel.followersListUsers.observe(viewLifecycleOwner, EventObserver(
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