package com.riyazuddin.zing.ui.main.fragments.chat

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.adapters.UserAdapterPagingData
import com.riyazuddin.zing.databinding.FragmentNewChatBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewChatFragment : Fragment(R.layout.fragment_new_chat) {

    private lateinit var binding: FragmentNewChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val args: NewChatFragmentArgs by navArgs()

    @Inject
    lateinit var usersAdapterPagingData: UserAdapterPagingData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentNewChatBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        subscribeToObservers()

        lifecycleScope.launch {
            viewModel.getFollowingAndFollowersUsers(Firebase.auth.uid!!).collect {
                usersAdapterPagingData.submitData(it)
            }
        }

        usersAdapterPagingData.setOnUserClickListener { user ->
            val bundle = Bundle().apply {
                putSerializable("otherEndUser", user)
                putSerializable("currentUser", args.currentUser)
            }
            findNavController().navigate(R.id.action_newChatFragment_to_chatFragment, bundle)
        }
    }

    private fun subscribeToObservers() {

    }

    private fun setupRecyclerView() {
        binding.rvFollowers.apply {
            adapter = usersAdapterPagingData
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }
}