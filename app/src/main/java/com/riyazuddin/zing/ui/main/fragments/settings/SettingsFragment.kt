package com.riyazuddin.zing.ui.main.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)

        val appSharedPreferences = requireActivity().getSharedPreferences("AppSharedPreferences",0)
        val sharedPreferencesEdit: SharedPreferences.Editor = appSharedPreferences.edit()
        val isNightModeNo: Boolean = appSharedPreferences.getBoolean("NightMode",false)

        if (isNightModeNo){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding.btnSwitch.setOnClickListener {
            if (isNightModeNo){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                sharedPreferencesEdit.putBoolean("NightMode",false)
                sharedPreferencesEdit.apply()
            }else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                sharedPreferencesEdit.putBoolean("NightMode",true)
                sharedPreferencesEdit.apply()
            }
        }

        binding.btnProfileInfo.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profileInfo)
        }
        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_currentPasswordVerification)
        }
    }
}