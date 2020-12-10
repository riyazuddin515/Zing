package com.riyazuddin.zing.ui.auth.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var binding: FragmentLoginBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        binding.tvRegister.setOnClickListener {
            if (findNavController().previousBackStackEntry != null)
                findNavController().popBackStack()
            else
             findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

    }
}