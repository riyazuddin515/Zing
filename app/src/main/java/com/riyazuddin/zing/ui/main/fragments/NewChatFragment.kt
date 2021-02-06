package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.data.ChatFragmentData
import com.riyazuddin.zing.databinding.FragmentNewChatBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NewChatFragment : Fragment(R.layout.fragment_new_chat) {

    private lateinit var binding: FragmentNewChatBinding

    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var usersAdapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewChatBinding.bind(view)

        viewModel.getFollowersList(Firebase.auth.uid!!)

        setupRecyclerView()
        subscribeToObservers()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        usersAdapter.setOnUserClickListener { user ->

            val bundle = Bundle().apply {
                putSerializable(
                    "chatFragmentData",
                    ChatFragmentData(user.uid, user.name, user.username, user.profilePicUrl)
                )
            }
            findNavController().navigate(R.id.action_newChat_to_chat, bundle)
        }
    }

    private fun subscribeToObservers() {
        viewModel.followersList.observe(viewLifecycleOwner, EventObserver(
            onLoading = {
                binding.progressBar.isVisible = true
            },
            onError = {
                binding.progressBar.isVisible = false
                snackBar(it)
            }
        ) { followersList ->
            binding.progressBar.isVisible = false
            usersAdapter.users = followersList
        })
    }

    private fun setupRecyclerView() {
        binding.rvFollowers.apply {
            adapter = usersAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }
}