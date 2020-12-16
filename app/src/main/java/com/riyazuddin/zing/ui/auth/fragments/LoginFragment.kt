package com.riyazuddin.zing.ui.auth.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentLoginBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.viewmodels.AuthViewModel
import com.riyazuddin.zing.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        subscribeToObservers()

        binding.tvRegister.setOnClickListener {
            if (findNavController().previousBackStackEntry != null)
                findNavController().popBackStack()
            else
             findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPassword)
        }

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.TIEEmail.text.toString(),
                binding.TIEPassword.text.toString()
            )
        }

    }

    private fun subscribeToObservers(){
         viewModel.loginStatus.observe(viewLifecycleOwner, EventObserver(
             onError = {
                 binding.progressBar.isVisible = false
                 snackBar(it)
             },
             onLoading = { binding.progressBar.isVisible = true }
         ){
             binding.progressBar.isVisible = false

             val currentUser = Firebase.auth.currentUser
             currentUser?.let { user ->
                 user.reload()
                 if (user.isEmailVerified){

                     Intent(requireActivity(), MainActivity::class.java).apply {
                         startActivity(this)
                         requireActivity().finish()
                     }
                 }else{
                     Snackbar.make(requireView(),"Verify Email",Snackbar.LENGTH_LONG)
                         .setAction("Send Email"){
                             user.sendEmailVerification()
                         }.show()
                 }



             }
         })
    }
}