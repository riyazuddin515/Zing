package com.riyazuddin.zing.ui.main.fragments.chat

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
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapterPagingData
import com.riyazuddin.zing.databinding.FragmentNewChatBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentNewChatBinding.inflate(layoutInflater)

        viewModel.setUid(args.currentUser.uid)
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

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.flow.collect {
                usersAdapterPagingData.submitData(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            usersAdapterPagingData.loadStateFlow.collect {
                binding.linearProgressIndicatorFirstLoad.isVisible = it.refresh is LoadState.Loading
                binding.linearProgressIndicatorLoadMore.isVisible = it.append is LoadState.Loading
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