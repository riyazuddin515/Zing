package com.riyazuddin.zing.ui.auth.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentForgotPasswordBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPassword: Fragment(R.layout.fragment_forgot_password) {

    private lateinit var binding: FragmentForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentForgotPasswordBinding.bind(view)

        subscribeToObservers()

        binding.btnSendMail.setOnClickListener {
            viewModel.sendPasswordResetLink(
                binding.TIEEmail.text.toString()
            )
        }
    }

    private fun subscribeToObservers() {
        viewModel.passwordResetStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                snackBar(it)
            },
            onLoading = { binding.progressBar.isVisible = true }
        ){
            binding.progressBar.isVisible = false
            snackBar(it)
        })
    }
}