package com.riyazuddin.zing.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)

        binding.btnProfileInfo.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profileInfo)
        }
        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_currentPasswordVerification)
        }
    }
}