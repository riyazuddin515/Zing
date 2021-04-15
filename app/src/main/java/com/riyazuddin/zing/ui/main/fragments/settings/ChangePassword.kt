package com.riyazuddin.zing.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentChangePasswordBinding
import com.riyazuddin.zing.other.Constants
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

            val newPassword = binding.TIENewPassword.text.toString()
            val confirmNewPassword = binding.TIERepeatNewPassword.text.toString()

            if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_fields_can_not_be_empty),
                    Toast.LENGTH_SHORT
                ).show()
            } else if (newPassword.length < Constants.MIN_PASSWORD) {
                binding.TIENewPassword.error = getString(
                    R.string.error_password_too_short,
                    Constants.MIN_PASSWORD
                )
            } else if (newPassword != confirmNewPassword) {
                binding.TIERepeatNewPassword.error = getString(R.string.error_password_not_match)
            } else {
                it.isEnabled = false
                viewModel.changePassword(
                    binding.TIENewPassword.text.toString(),
                    binding.TIERepeatNewPassword.text.toString()
                )
            }
        }

    }

    private fun subscribeToObservers() {
        viewModel.changePasswordStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                binding.btnUpdate.isEnabled = true
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) {
            binding.progressBar.isVisible = false
            binding.btnUpdate.isEnabled = true
            snackBar(it.toString())
            findNavController().navigateUp()
        })
    }
}