package com.riyazuddin.zing.ui.main.fragments.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentSettingsBinding
import com.riyazuddin.zing.other.Constants.PRIVATE
import com.riyazuddin.zing.other.Constants.PUBLIC
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.NavGraphArgsConstants.PRIVACY_POLICY_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.TERMS_AND_CONDITIONS_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.TYPE_ARG
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.bottomsheet.AccountPrivacyBottomSheetFragment
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.fragments.HomeFragment
import com.riyazuddin.zing.ui.main.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.ChatClient
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    companion object {
        const val TAG = "SettingsFragment"
    }

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    private var currentUser: User? = null

    @Inject
    lateinit var chatClient: ChatClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.auth.uid?.let {
            viewModel.getUserProfile(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)

        binding.switchPrivateAccount.isClickable = currentUser != null

        powerOptimization()
        subscribeToObservers()
        setClickListeners()


        val version = "Version : ${com.riyazuddin.zing.BuildConfig.VERSION_NAME}"
        binding.tvVersion.text = version
    }

    private fun subscribeToObservers() {
        viewModel.userProfileStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                Log.e(TAG, "subscribeToObservers: $it")
            }
        ) {
            binding.switchPrivateAccount.isClickable = true
            currentUser = it
            binding.switchPrivateAccount.isChecked = it.privacy != PUBLIC
        })

        viewModel.togglePrivacyStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {
                Toast.makeText(requireContext(), "Changing Account Privacy", Toast.LENGTH_SHORT)
                    .show()
            }
        ) { privacy ->
            currentUser?.let {
                it.privacy = privacy
            }
            binding.switchPrivateAccount.isChecked = privacy == PRIVATE
        })

        viewModel.removeDeviceTokeStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                Log.e(TAG, "subscribeToObservers: $it")
            },
            onLoading = {
                Toast.makeText(requireContext(), "Logging Out", Toast.LENGTH_SHORT).show()
            }
        ) {
            if (it) {

                chatClient.disconnect()
                Firebase.auth.signOut()
                Intent(requireActivity(), AuthActivity::class.java).apply {
                    startActivity(this)
                    requireActivity().finish()
                }
            } else {
                snackBar("can't logout. Try again")
            }
        })
    }

    private fun checkForUpdate() {
        val appVersion = getAppVersion()
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val latestVersion = remoteConfig.getString(getString(R.string.latest_version_of_app))
        if (latestVersion.isNotEmpty() && appVersion.isNotEmpty()) {
            val appVersionWithoutAlphaNumeric = appVersion.replace(".", "")
            val latestVersionWithoutAlphaNumeric = latestVersion.replace(".", "")
            try {
                val appVersionInt = appVersionWithoutAlphaNumeric.toInt()
                val latestVersionInt = latestVersionWithoutAlphaNumeric.toInt()
                if (latestVersionInt > appVersionInt) {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.MaterialAlertDialog_Round
                    ).apply {
                        setIcon(R.drawable.ic_update)
                        setTitle(getString(R.string.new_update_available))
                        setMessage(getString(R.string.update_message))
                        setPositiveButton(getString(R.string.download)) { _, _ ->
                            val newVersionUrl =
                                remoteConfig.getString(getString(R.string.new_version_url))
                            val request = DownloadManager.Request(Uri.parse(newVersionUrl))
                            val title = "Zing-${latestVersionWithoutAlphaNumeric}.apk"
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setMimeType("application/vnd.android.package-archive")
                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                title
                            )
                            val manager =
                                requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            manager.enqueue(request)
                            Toast.makeText(
                                requireContext(),
                                resources.getString(R.string.downloading),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                    }.show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.app_up_to_data),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkForUpdate: ", e)
            }
        }
    }

    private fun getAppVersion(): String {
        var result: String? = null
        try {
            result = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            ).versionName
        } catch (e: Exception) {
            Log.e(TAG, "getApVersion: ", e)
        }
        return result ?: ""
    }

    private fun setClickListeners() {
        binding.tvEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profileInfo)
        }
        binding.tvnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_currentPasswordVerification)
        }
        binding.tvCheckForUpdates.setOnClickListener {
            checkForUpdate()
        }
        binding.btnLogout.setOnClickListener {
            CustomDialog(
                getString(R.string.log_out),
                getString(R.string.log_out_message),
                getString(R.string.logOut),
                getString(R.string.cancel)
            ).apply {
                setPositiveListener {
                    viewModel.removeDeviceToken(Firebase.auth.uid!!)
                }
            }.show(parentFragmentManager, null)
        }
        binding.switchPrivateAccount.setOnClickListener {
            binding.switchPrivateAccount.isChecked = !binding.switchPrivateAccount.isChecked
            currentUser?.let {
                AccountPrivacyBottomSheetFragment(
                    if (it.privacy == PUBLIC) PRIVATE
                    else PUBLIC
                ).apply {
                    onClickListener {
                        viewModel.toggleAccountPrivacy(
                            it.uid,
                            if (it.privacy == PUBLIC) PRIVATE
                            else PUBLIC
                        )
                        dismiss()
                    }
                }.show(parentFragmentManager, tag)
            }
        }
        binding.tvPrivacyPolicy.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_privacyPolicyAndTermsAndConditionsFragment,
                Bundle().apply {
                    putString(TYPE_ARG, PRIVACY_POLICY_ARG)
                }
            )
        }
        binding.tvTermsAndConditions.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_privacyPolicyAndTermsAndConditionsFragment,
                Bundle().apply {
                    putString(TYPE_ARG, TERMS_AND_CONDITIONS_ARG)
                }
            )
        }
    }

    private fun powerOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = requireContext().getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                binding.switchBatteryOptimization.isVisible = true
                binding.switchBatteryOptimization.isChecked = false
                binding.switchBatteryOptimization.setOnClickListener {
                    binding.switchBatteryOptimization.isChecked =
                        !binding.switchBatteryOptimization.isChecked
                    CustomDialog(
                        getString(R.string.warning),
                        getString(R.string.battery_optimization_message),
                        getString(R.string.turn_off),
                        getString(R.string.cancel)
                    ).apply {
                        setPositiveListener {
                            val intent =
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            startActivity(intent)
                        }
                    }.show(childFragmentManager, null)
                }
            } else
                binding.switchBatteryOptimization.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        powerOptimization()
    }
}