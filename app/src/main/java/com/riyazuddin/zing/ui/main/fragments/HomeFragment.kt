package com.riyazuddin.zing.ui.main.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentHomeBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : BasePostFragment(R.layout.fragment_home) {

    override val source: String
        get() = (R.id.homeFragment).toString()

    private lateinit var binding: FragmentHomeBinding

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: HomeViewModel by viewModels()
            return vm
        }

//    private val viewModel: HomeViewModel by lazy { basePostViewModel as HomeViewModel }
    private lateinit var viewModel: HomeViewModel

    private var currentUser: User? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHomeBinding.bind(view)

        viewModel = basePostViewModel as HomeViewModel

        subscribeToObservers()
        setUpRecyclerView()

        binding.btnLogout.setOnClickListener {
            CustomDialog("Log Out", " Are you sure to logout of the app?").apply {
                setPositiveListener {
                    Firebase.auth.signOut()
                    Intent(requireActivity(), AuthActivity::class.java).apply {
                        startActivity(this)
                        requireActivity().finish()
                    }
                }
            }.show(parentFragmentManager, null)
        }

        binding.ibRecentChat.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable("currentUser", currentUser)
            }
            findNavController().navigate(R.id.action_homeFragment_to_recentChatListFragment, bundle)
        }

        lifecycleScope.launch {
            viewModel.pagingFlow.collect {
                postAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest {
                binding.progressBar.isVisible =
                    it.refresh is LoadState.Loading ||
                            it.append is LoadState.Loading
            }
        }

        viewModel.loadCurrentUser(Firebase.auth.uid!!)

        postAdapter.setOnUserClickListener {
            findNavController().navigate(
                HomeFragmentDirections.globalActionToOthersProfileFragment(
                    it.postedBy
                )
            )
        }
    }

    private fun subscribeToObservers(){
        viewModel.loadCurrentUserStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = { snackBar(it) },
            onLoading = { Log.i(TAG, "subscribeToObservers: loading current user") }
        ) {
            currentUser = it
            binding.ibRecentChat.isVisible = true
        })
    }

    private fun setUpRecyclerView() {
        binding.rvPost.apply {
            adapter = postAdapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            layoutManager = LinearLayoutManager(requireContext())
//            itemAnimator = null
        }
    }

    companion object{
        const val TAG = "HomeFragment"
    }

}