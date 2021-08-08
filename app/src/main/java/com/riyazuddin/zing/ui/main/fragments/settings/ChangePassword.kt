package com.riyazuddin.zing.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentChangePasswordBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.Validator
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePassword : Fragment(R.layout.fragment_change_password) {

    private lateinit var validator: Validator
    private lateinit var binding: FragmentChangePasswordBinding

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChangePasswordBinding.bind(view)
        validator = Validator(requireContext())

        subscribeToObservers()

        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.TIENewPassword.addTextChangedListener {
            validator.validatePassword(it.toString()).apply {
                if (this != Constants.VALID)
                    binding.TILNewPassword.error = this
                else
                    binding.TILNewPassword.error = null
            }
        }
        binding.TIERepeatNewPassword.addTextChangedListener {
            validator.validatePassword(it.toString()).apply {
                if (this != Constants.VALID)
                    binding.TILRepeatNewPassword.error = this
                else
                    binding.TILRepeatNewPassword.error = null
            }
        }

        binding.btnUpdate.setOnClickListener {

            val newPassword = binding.TIENewPassword.text.toString()
            val confirmNewPassword = binding.TIERepeatNewPassword.text.toString()

            validator.validatePassword(newPassword).apply {
                if (this != Constants.VALID) {
                    binding.TILNewPassword.error = this
                    return@setOnClickListener
                }
            }
            if (confirmNewPassword != newPassword) {
                binding.TILRepeatNewPassword.error = this.getString(R.string.error_password_not_match)
                return@setOnClickListener
            }

            it.isVisible = false
            viewModel.changePassword(binding.TIENewPassword.text.toString())
        }

    }

    private fun subscribeToObservers() {
        viewModel.changePasswordStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                binding.btnUpdate.isVisible = true
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) {
            binding.progressBar.isVisible = false
            binding.btnUpdate.isVisible = true
            snackBar(it)
            findNavController().popBackStack()
        })
    }
}