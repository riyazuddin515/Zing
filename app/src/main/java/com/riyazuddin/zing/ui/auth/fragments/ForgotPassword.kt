package com.riyazuddin.zing.ui.auth.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentForgotPasswordBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPassword : Fragment(R.layout.fragment_forgot_password) {

    private lateinit var binding: FragmentForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentForgotPasswordBinding.bind(view)

        subscribeToObservers()

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSendMail.setOnClickListener {

            val email = binding.TIEEmail.text.toString()

            if (email.isEmpty())
                binding.TILEmail.error = this.getString(R.string.error_fields_can_not_be_empty)
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                binding.TILEmail.error = this.getString(R.string.error_not_a_valid_email)
            else {
                it.isEnabled = false
                viewModel.sendPasswordResetLink(
                    binding.TIEEmail.text.toString()
                )
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.passwordResetStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                binding.btnSendMail.isEnabled = true
                snackBar(it)
            },
            onLoading = { binding.progressBar.isVisible = true }
        ) {
            binding.progressBar.isVisible = false
            binding.btnSendMail.isEnabled = true
            snackBar(it)
        })
    }
}