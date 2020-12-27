package com.riyazuddin.zing.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment

        binding.bottomNavigation.apply {
            background = null
            setupWithNavController(navHostFragment.findNavController())
            setOnNavigationItemReselectedListener { }
        }

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.searchFragment, R.id.createPostFragment, R.id.profileFragment, R.id.settingsFragment, R.id.othersProfileFragment ->
                    binding.bottomNavigation.visibility = View.VISIBLE
                else -> binding.bottomNavigation.visibility = View.GONE
            }
        }

    }

    fun showBottomNavigation() {
        binding.bottomNavigation.isVisible = true
    }

    fun hideBottomNavigation() {
        binding.bottomNavigation.isVisible = false
    }
}