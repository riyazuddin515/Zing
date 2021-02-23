package com.riyazuddin.zing.ui.main.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentHomeBinding
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.toolbar.inflateMenu(R.menu.top_menu)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.logOut) {
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
            if (it.itemId == R.id.chatList) {
                findNavController().navigate(R.id.action_homeFragment_to_chatList)
            }
            true
        }

        return view
    }

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: HomeViewModel by viewModels()
            return vm
        }

    private val viewModel: HomeViewModel by lazy { basePostViewModel as HomeViewModel }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()

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

        postAdapter.setOnUserClickListener {
            findNavController().navigate(
                HomeFragmentDirections.globalActionToOthersProfileFragment(
                    it.postedBy
                )
            )
        }

    }

    private fun setUpRecyclerView() {
        binding.rvPost.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

}