package com.riyazuddin.zing.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.databinding.ActivityAuthBinding
import com.riyazuddin.zing.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val appSharedPreferences = getSharedPreferences("AppSharedPreferences", 0)
//        val isNightModeNo: Boolean = appSharedPreferences.getBoolean("NightMode", false)
//
//        if (isNightModeNo) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//        } else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//        }

    }

    override fun onStart() {
        super.onStart()
        Firebase.auth.currentUser?.let {
            it.reload()
            if (it.isEmailVerified) {
                Intent(this, MainActivity::class.java).apply {
                    startActivity(this)
                    finish()
                }
            }
        }
    }
}