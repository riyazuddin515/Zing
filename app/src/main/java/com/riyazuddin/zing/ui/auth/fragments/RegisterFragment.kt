package com.riyazuddin.zing.ui.auth.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var binding: FragmentRegisterBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRegisterBinding.bind(view)

        binding.tvLogin.setOnClickListener {
            if (findNavController().previousBackStackEntry != null)
                findNavController().popBackStack()
            else
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }
}