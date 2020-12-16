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
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentRegisterBinding
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
                    viewModel.searchUsername(it.toString())
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            viewModel.register(
                binding.TIEName.text.toString(),
                binding.TIEUsername.text.toString(),
                binding.TIEEmail.text.toString(),
                binding.TIEPassword.text.toString(),
                binding.TIERepeatPassword.text.toString()
            )
        }
    }

    private fun subscribeToObservers(){
        viewModel.registerStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) {
            binding.progressBar.isVisible = false
            snackBar(getString(R.string.registration_success))
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
                binding.btnRegister.isEnabled = false
            }
        ){
            binding.btnRegister.isEnabled = true
            binding.TILUsername.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.TILUsername.endIconDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_outline_check_circle, null)
        })
    }
}