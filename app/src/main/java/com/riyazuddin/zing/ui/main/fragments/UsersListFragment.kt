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
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapterPagingData
import com.riyazuddin.zing.databinding.FragmentUsersBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.UsersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
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

        viewModel.getListOfUsersUid(args.id, args.title)
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
        subscribeToObservers()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
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

    private fun subscribeToObservers() {
        viewModel.listOfUsersUid.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            }
        ) { list ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getFlowOfUsers(list).collect {
                    userAdapterPagingData.submitData(it)
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
        })
    }

    private fun setupRecyclerView() {
        binding.rvUsers.apply {
            adapter = userAdapterPagingData
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
}