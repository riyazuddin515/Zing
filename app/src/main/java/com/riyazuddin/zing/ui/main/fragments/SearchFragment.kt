package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentSearchBinding
import com.riyazuddin.zing.other.Constants.SEARCH_TIME_DELAY
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.ui.main.viewmodels.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var binding: FragmentSearchBinding

    @Inject
    lateinit var userAdapter: UserAdapter
    private val viewModel: SearchViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view)

        setUpRecyclerView()
        subscribeToObservers()

        var job: Job? = null
        binding.TIESearch.addTextChangedListener { eiditable ->
            job?.cancel()
            job = lifecycleScope.launch {
                delay(SEARCH_TIME_DELAY)
                eiditable?.let {
//                    viewModel.firebaseUserSearch(it.toString())
                    viewModel.search(it.toString())
                }
            }
        }

        userAdapter.setOnUserClickListener { user ->
            if (Firebase.auth.uid != user.uid) {
                findNavController().navigate(
                    SearchFragmentDirections.globalActionToOthersProfileFragment(
                        user.uid
                    )
                )
            } else {
                findNavController().navigate(R.id.profileFragment)
            }

        }
    }

    private fun subscribeToObservers() {
//        viewModel.firebaseUserSearchResult.observe(viewLifecycleOwner, EventObserver(
//            onError = {
//                snackBar(it)
//            }
//        ){
//            userAdapter.users = it
//        })
        viewModel.algoliaSearchResult.observe(viewLifecycleOwner, EventObserver(
            onError = { binding.progressBar.isVisible = false },
            onLoading = { binding.progressBar.isVisible = true }
        ) {
            val hits = it.hits
            val users = mutableListOf<User>()
            val gson = Gson()
            hits.forEach { hit ->
                val u = gson.fromJson(hit.json.toString(), User::class.java)
                u.uid = hit.json.getValue("objectID").toString().replace("\"", "")
                users.add(u)
            }
            binding.progressBar.isVisible = false
            userAdapter.users = users
        })
    }

    private fun setUpRecyclerView() {
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

    companion object {
        const val TAG = "SearchFragment"
    }
}