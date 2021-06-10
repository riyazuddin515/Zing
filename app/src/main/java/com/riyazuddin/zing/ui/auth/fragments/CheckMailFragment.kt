package com.riyazuddin.zing.ui.auth.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCheckMailBinding


class CheckMailFragment : Fragment(R.layout.fragment_check_mail) {

    private lateinit var binding: FragmentCheckMailBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCheckMailBinding.bind(view)

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnOpenMail.setOnClickListener {
            Intent(Intent.ACTION_MAIN).apply {
                this.addCategory(Intent.CATEGORY_APP_EMAIL)
                this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
                findNavController().popBackStack()
            }
        }
    }
}