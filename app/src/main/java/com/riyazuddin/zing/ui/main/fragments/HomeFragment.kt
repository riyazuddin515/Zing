package com.riyazuddin.zing.ui.main.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.adapters.UserAdapter
import com.riyazuddin.zing.databinding.FragmentHomeBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BasePostFragment(R.layout.fragment_home) {

    override val sourceToDestinationLayoutId: Int
        get() = R.id.action_homeFragment_to_likedByFragment

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
            true
        }

        return view
    }

    override val postProgressBar: ProgressBar
        get() = binding.progressBar
    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: HomeViewModel by viewModels()
            return vm
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {
        binding.rvPost.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

}