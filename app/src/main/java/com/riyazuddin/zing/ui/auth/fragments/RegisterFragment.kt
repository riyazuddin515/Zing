package com.riyazuddin.zing.ui.auth.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentRegisterBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.MIN_USERNAME
import com.riyazuddin.zing.other.Constants.SEARCH_TIME_DELAY
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRegisterBinding.bind(view)

        subscribeToObservers()

        binding.tvLogin.setOnClickListener {
            if (findNavController().previousBackStackEntry != null)
                findNavController().popBackStack()
            else
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        var job: Job? = null
        binding.TIEUsername.addTextChangedListener { editable ->
            job?.cancel()
            job = lifecycleScope.launch {
                delay(SEARCH_TIME_DELAY)
                editable?.let {
                    if (it.isEmpty() || it.length < MIN_USERNAME) {
                        binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
                    } else if (it.contains(" ")) {
                        binding.TILUsername.error = "No space allowed"
                    } else if (it.length >= MIN_USERNAME) {
//                        viewModel.searchUsername(it.toString())
                        viewModel.checkUserNameAvailability(it.toString())
                    }
                }
            }
        }

        binding.TIEEmail.addTextChangedListener {
            binding.TIEEmail.error = null
        }

        binding.btnRegister.setOnClickListener {

            val name = binding.TIEName.text.toString()
            val username = binding.TIEUsername.text.toString()
            val email = binding.TIEEmail.text.toString()
            val password = binding.TIEPassword.text.toString()
            val repeatPassword = binding.TIERepeatPassword.text.toString()

            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_fields_can_not_be_empty),
                    Toast.LENGTH_SHORT
                ).show()
            else if (username.length < MIN_USERNAME)
                binding.TILUsername.error = this.getString(
                    R.string.error_username_too_long,
                    MIN_USERNAME
                )
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                binding.TILEmail.error = this.getString(R.string.error_not_a_valid_email)
            else if (password.length < Constants.MIN_PASSWORD)
                binding.TILPassword.error = this.getString(
                    R.string.error_password_too_short,
                    Constants.MIN_PASSWORD
                )
            else if (repeatPassword != password)
                binding.TILRepeatPassword.error = this.getString(R.string.error_password_not_match)
            else {
                it.isEnabled = false
                viewModel.register(
                    binding.TIEName.text.toString(),
                    binding.TIEUsername.text.toString(),
                    binding.TIEEmail.text.toString(),
                    binding.TIEPassword.text.toString()
                )
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.registerStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                binding.btnRegister.isEnabled = true
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) {
            binding.progressBar.isVisible = false
            binding.btnRegister.isEnabled = true
            snackBar(getString(R.string.registration_success))
            Firebase.auth.signOut()
//            findNavController().navigate(R.id.action_registerFragment_to_checkMailFragment)
        })

        viewModel.isUsernameAvailable.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
                binding.TILUsername.error = it
            },
            onLoading = {
                binding.TILUsername.error = null
                binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
                binding.btnRegister.isEnabled = false
            }
        ) {
            binding.btnRegister.isEnabled = true
            binding.TILUsername.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.TILUsername.endIconDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_outline_check_circle, null)
        })
    }
}