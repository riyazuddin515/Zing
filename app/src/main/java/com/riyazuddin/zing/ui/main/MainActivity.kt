package com.riyazuddin.zing.ui.main

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.ActivityMainBinding
import com.riyazuddin.zing.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
//        powerOptimization()
    }

    companion object {
        const val TAG = "MainActivityTag"
    }
}