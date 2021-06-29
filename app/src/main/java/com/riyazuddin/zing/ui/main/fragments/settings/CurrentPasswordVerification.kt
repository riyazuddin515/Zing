package com.riyazuddin.zing.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCurrentPasswordVerificationBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CurrentPasswordVerification : Fragment(R.layout.fragment_current_password_verification) {

    private lateinit var binding: FragmentCurrentPasswordVerificationBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCurrentPasswordVerificationBinding.bind(view)

        subscribeToObservers()

        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnVerify.setOnClickListener {
            it.isVisible = false
            viewModel.verifyAccount(
                binding.TIECurrentPassword.text.toString()
            )
        }

    }

    private fun subscribeToObservers() {
        viewModel.currentPasswordVerificationStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                binding.TILCurrentPassword.error = it
                binding.btnVerify.isVisible = true
                snackBar(it)
            },
            onLoading = { binding.progressBar.isVisible = true }
        ) {
            binding.progressBar.isVisible = false
            binding.btnVerify.isVisible = true
            findNavController().navigate(R.id.action_currentPasswordVerification_to_changePassword)
        })
    }
}