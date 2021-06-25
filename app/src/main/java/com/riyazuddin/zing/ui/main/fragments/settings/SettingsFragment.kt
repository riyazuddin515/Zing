package com.riyazuddin.zing.ui.main.fragments.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
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
        binding.btnCheckForUpdates.setOnClickListener {
            get()
        }
    }

    private fun get() {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val newVersionCode = mFirebaseRemoteConfig.getString("new_version_code")

                val currentVersionCode = requireContext().packageManager.getPackageInfo(
                    requireContext().packageName,
                    0
                ).longVersionCode

                if (Integer.parseInt(newVersionCode) > currentVersionCode) {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.MaterialAlertDialog_Round
                    ).apply {
                        setIcon(R.drawable.ic_update)
                        setTitle("New Update Available")
                        setMessage("Update the app to the latest version for new features and bug fix. Note: UnInstall current app and install new apk from Downloads Directory")
                        setPositiveButton("Update") { _, _ ->
                            val newVersionUrl = mFirebaseRemoteConfig.getString("new_version_url")

                            val request = DownloadManager.Request(Uri.parse(newVersionUrl))
                            val title = "Zing-${newVersionCode}.apk"
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                title
                            )

                            val manager =
                                requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            manager.enqueue(request)
                            Toast.makeText(requireContext(), "Downloading...", Toast.LENGTH_SHORT)
                                .show()

                        }
                        setNegativeButton("Cancel") { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                    }.show()
                } else {
                    Toast.makeText(requireContext(), "App is up-to date", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}