package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentLikedByBinding

class LikedByFragment : Fragment(R.layout.fragment_liked_by) {

    private val args: LikedByFragmentArgs by navArgs()
    private lateinit var binding: FragmentLikedByBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLikedByBinding.bind(view)

        setUpRecyclerView()

        args.userAdapter.setOnUserClickListener {
            if (Firebase.auth.uid != it.uid){
                findNavController().navigate(LikedByFragmentDirections.globalActionToOthersProfileFragment(it.uid))
            }
            else findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setUpRecyclerView(){
        binding.rvUsers.apply {
            adapter = args.userAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

}