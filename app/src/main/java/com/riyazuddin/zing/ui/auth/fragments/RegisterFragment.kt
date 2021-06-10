package com.riyazuddin.zing.ui.auth.fragments

import android.os.Bundle
import android.view.View
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
import com.riyazuddin.zing.other.Constants.SEARCH_TIME_DELAY
import com.riyazuddin.zing.other.Constants.VALID
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.Validator
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var validator: Validator
    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRegisterBinding.bind(view)

        validator = Validator(requireContext())
        subscribeToObservers()

        binding.tvLogin.setOnClickListener {
            if (findNavController().previousBackStackEntry != null)
                findNavController().popBackStack()
            else
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

//        var job: Job? = null
//        binding.TIEUsername.addTextChangedListener { editable ->
//            binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
//            binding.TILUsername.error = null
//            job?.cancel()
//            job = lifecycleScope.launch {
//                delay(SEARCH_TIME_DELAY)
//                editable?.let {
//                    validator.validateUsername(it.toString()).apply {
//                        if (this != VALID) {
//                            binding.TILUsername.error = this
//                        } else {
//                            viewModel.checkUserNameAvailability(it.toString())
//                        }
//                    }
//                }
//            }
//        }

        binding.TIEName.addTextChangedListener {
            validator.validateName(it.toString()).apply {
                if (this != VALID)
                    binding.TILName.error = this
                else
                    binding.TILName.error = null
            }
        }

        binding.TIEEmail.addTextChangedListener {
            validator.validateEmail(it.toString()).apply {
                if (this != VALID)
                    binding.TILEmail.error = this
                else
                    binding.TILEmail.error = null

                binding.TILName.error
            }
        }
        binding.TIEPassword.addTextChangedListener {
            validator.validatePassword(it.toString()).apply {
                if (this != VALID)
                    binding.TILPassword.error = this
                else
                    binding.TILPassword.error = null
            }
        }
        binding.TIERepeatPassword.addTextChangedListener {
            if (it.toString() != binding.TIEPassword.text.toString()) {
                binding.TILRepeatPassword.error = this.getString(R.string.error_password_not_match)
            } else
                binding.TILRepeatPassword.error = null
        }


        binding.btnRegister.setOnClickListener {

            val name = binding.TIEName.text.toString().trim()
            val username = binding.TIEUsername.text.toString().trim()
            val email = binding.TIEEmail.text.toString().trim()
            val password = binding.TIEPassword.text.toString().trim()
            val repeatPassword = binding.TIERepeatPassword.text.toString().trim()

            validator.validateName(name).apply {
                if (this != VALID) {
                    binding.TILName.error = this
                    return@setOnClickListener
                }
            }

//            validator.validateUsername(username).apply {
//                if (this != VALID) {
//                    binding.TILUsername.error = this
//                    return@setOnClickListener
//                }
//            }

            validator.validateEmail(email).apply {
                if (this != VALID) {
                    binding.TILEmail.error = this
                    return@setOnClickListener
                }
            }

            validator.validatePassword(password).apply {
                if (this != VALID) {
                    binding.TILPassword.error = this
                    return@setOnClickListener
                }
            }

            if (repeatPassword != password) {
                binding.TILRepeatPassword.error = this.getString(R.string.error_password_not_match)
                return@setOnClickListener
            }

            it.isVisible = false
            viewModel.register(name, username, email, password)
        }
    }

    private fun subscribeToObservers() {
        viewModel.registerStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                binding.btnRegister.isVisible = true
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) {
            binding.progressBar.isVisible = false
            snackBar(getString(R.string.registration_success))
            Firebase.auth.signOut()
            findNavController().navigate(R.id.action_registerFragment_to_checkMailFragment)
        })

        viewModel.isUsernameAvailable.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
                binding.TILUsername.error = it
            },
            onLoading = {
                binding.TILUsername.error = null
                binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
            }
        ) {
            binding.TILUsername.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.TILUsername.endIconDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_outline_check_circle, null)
        })
    }


}