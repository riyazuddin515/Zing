package com.riyazuddin.zing.ui.main.fragments.stream_chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapterPagingData
import com.riyazuddin.zing.databinding.FragmentNewChatBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.fragments.BindingFragment
import com.riyazuddin.zing.ui.main.viewmodels.GetStreamViewModel
import com.riyazuddin.zing.ui.main.viewmodels.NewChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewChatFragment : BindingFragment<FragmentNewChatBinding>() {

    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentNewChatBinding::inflate

    private val viewModel: NewChatViewModel by viewModels()
    private val args: NewChatFragmentArgs by navArgs()

    private val getStreamViewModel: GetStreamViewModel by viewModels()

    @Inject
    lateinit var usersAdapterPagingData: UserAdapterPagingData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        setupRecyclerView()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.flow(args.currentUser.uid).collect {
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
            getStreamViewModel.createChatChannel(args.currentUser.uid, user.uid)
        }
    }

    private fun subscribeToObservers() {
        getStreamViewModel.createChannelStatus.observe(viewLifecycleOwner, EventObserver(
            onLoading = { binding.progressCircularForCreateChannel.isVisible = true },
            onError = {
                binding.progressCircularForCreateChannel.isVisible = false
                snackBar(it)
            }
        ) {
            val bundle = Bundle().apply {
                putString("cid", it)
            }
            findNavController().navigate(R.id.action_newChatFragment_to_chatFragment, bundle)
        })
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