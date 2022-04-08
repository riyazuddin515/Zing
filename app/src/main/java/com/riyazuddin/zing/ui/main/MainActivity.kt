package com.riyazuddin.zing.ui.main

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.ActivityMainBinding
import com.riyazuddin.zing.services.ZingFirebaseMessagingService
import com.riyazuddin.zing.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.ChatClient
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var chatClient: ChatClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment

        binding.bottomNavigation.apply {
            setupWithNavController(navHostFragment.findNavController())
            setOnNavigationItemReselectedListener {}
        }

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.searchFragment,
                R.id.profileFragment ->
                    binding.bottomNavigation.visibility = View.VISIBLE

                else -> binding.bottomNavigation.visibility = View.GONE
            }
            if (destination.id == R.id.profileFragment) {
                binding.bottomNavigation.getBadge(R.id.profileFragment).let {
                    binding.bottomNavigation.removeBadge(R.id.profileFragment)
                    it?.isVisible = false
                }
            }
        }

        //Removing all existing notification
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        checkForBatteryRestrictions()

        val componentName = ComponentName(this, ZingFirebaseMessagingService::class.java)
        packageManager.setComponentEnabledSetting(
            componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

    }

    override fun onStart() {
        super.onStart()
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            Intent(this, AuthActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        } else {
            auth.currentUser?.let {
                it.reload()
                if (!it.isEmailVerified) {
                    auth.signOut()
                    Intent(this, AuthActivity::class.java).apply {
                        startActivity(this)
                        finish()
                    }
                }
            }
        }
    }

    private fun checkForBatteryRestrictions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            if (activityManager.isBackgroundRestricted) {
                MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Round)
                    .setTitle("Battery Restriction Detected")
                    .setMessage("Allow App to work in the background so that it can receive notifications. Open settings, find and turn off battery restrictions for zing.")
                    .setPositiveButton("Open") { dialogInterface, _ ->
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                        startActivity(intent)
                        dialogInterface.cancel()
                    }
                    .setNegativeButton("Close") { dialogInterface, _ ->
                        dialogInterface.cancel()
                    }
                    .create()
                    .show()
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun checkForBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager?
            if (!pm!!.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        chatClient.disconnectSocket()
        chatClient.disconnect()
        super.onDestroy()
    }

    companion object {
        const val TAG = "MainActivityTag"
    }
}