package com.riyazuddin.zing.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentChangePasswordBinding
import com.riyazuddin.zing.other.Constants.MIN_PASSWORD
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePassword : Fragment(R.layout.fragment_change_password) {

    private lateinit var binding: FragmentChangePasswordBinding

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChangePasswordBinding.bind(view)

        subscribeToObservers()

        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnUpdate.setOnClickListener {
            if (binding.TIENewPassword.text.toString().length < MIN_PASSWORD) {
                binding.TIENewPassword.error =
                    requireContext().getString(R.string.error_password_too_short)
                return@setOnClickListener
            }

            if (binding.TIERepeatNewPassword.text.toString().length < MIN_PASSWORD) {
                binding.TIERepeatNewPassword.error =
                    requireContext().getString(R.string.error_password_too_short)
                return@setOnClickListener
            }

            if (binding.TIENewPassword.text.toString() != binding.TIERepeatNewPassword.text.toString()) {
                snackBar(requireContext().getString(R.string.error_password_not_match))
                return@setOnClickListener
            }

            viewModel.changePassword(
                binding.TIENewPassword.text.toString()
            )

        }

    }

    private fun subscribeToObservers() {
        viewModel.changePasswordStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) {
            binding.progressBar.isVisible = false
            snackBar(it.toString())
            findNavController().navigateUp()
        })
    }
}