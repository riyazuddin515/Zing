package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.databinding.FragmentLikedByBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LikedByFragment : Fragment(R.layout.fragment_liked_by) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: FragmentLikedByBinding

    private val args: LikedByFragmentArgs by navArgs()

    @Inject
    lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentLikedByBinding.inflate(layoutInflater)

        setUpRecyclerView()
        viewModel.getPostLikedUsers(args.postId)
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

        subscribeToObservers()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        userAdapter.setOnUserClickListener {
            if (Firebase.auth.uid == it.uid)
                findNavController().navigate(R.id.profileFragment)
            else
                findNavController().navigate(
                    CommentsFragmentDirections.globalActionToOthersProfileFragment(
                        it.uid
                    )
                )
        }
    }

    private fun subscribeToObservers() {
        viewModel.postLikedUsersStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = { snackBar(it) },
        ) {
            userAdapter.users = it
        })
    }

    private fun setUpRecyclerView() {
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

}