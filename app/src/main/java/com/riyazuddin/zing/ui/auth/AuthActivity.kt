package com.riyazuddin.zing.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.ActivityAuthBinding
import com.riyazuddin.zing.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onResume() {
        super.onResume()

        FirebaseAuth.getInstance().currentUser?.let {
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