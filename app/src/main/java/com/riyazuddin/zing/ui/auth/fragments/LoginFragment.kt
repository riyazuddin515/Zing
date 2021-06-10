package com.riyazuddin.zing.ui.auth.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentLoginBinding
import com.riyazuddin.zing.other.Constants.VALID
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.Validator
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.viewmodels.AuthViewModel
import com.riyazuddin.zing.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var validator: Validator

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        validator = Validator(requireContext())
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

        binding.TIEEmail.addTextChangedListener {
            validator.validateEmail(it.toString()).apply {
                if (this != VALID)
                    binding.TILEmail.error = this
                else
                    binding.TILEmail.error = null
            }
        }
        binding.TIEPassword.addTextChangedListener {
            binding.TILPassword.error = null
        }

        binding.btnLogin.setOnClickListener {

            val email = binding.TIEEmail.text.toString().trim()
            val password = binding.TIEPassword.text.toString().trim()

            validator.validateEmail(email).apply {
                if (this != VALID) {
                    binding.TILEmail.error = this
                    return@setOnClickListener
                }
            }

            if (password.isEmpty()) {
                binding.TILPassword.error = getString(R.string.error_password_empty)
                return@setOnClickListener
            }

            it.isVisible = false
            viewModel.login(email, password)
        }

    }

    private fun subscribeToObservers() {
        viewModel.loginStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                binding.btnLogin.isVisible = true
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) {
            binding.progressBar.isVisible = false
            val currentUser = Firebase.auth.currentUser
            currentUser?.let { user ->
                user.reload()
                if (user.isEmailVerified) {
                    Intent(requireActivity(), MainActivity::class.java).apply {
                        startActivity(this)
                        requireActivity().finish()
                    }
                } else {
                    Firebase.auth.signOut()
                    Snackbar.make(requireView(), "Verify Email", Snackbar.LENGTH_LONG)
                        .setAction("Send Email") {
                            user.sendEmailVerification()
                        }.show()
                    binding.btnLogin.isVisible = true
                }

            }
        })
    }

    companion object{
        const val TAG = "LoginFragment"
    }
}