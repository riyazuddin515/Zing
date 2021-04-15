package com.riyazuddin.zing.ui.auth.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCheckMailBinding


class CheckMailFragment: Fragment(R.layout.fragment_check_mail) {

    private lateinit var binding: FragmentCheckMailBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCheckMailBinding.bind(view)

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnOpenMail.setOnClickListener {
            val manager: PackageManager = requireContext().packageManager
            val i = manager.getLaunchIntentForPackage("com.google.android.gmail")
            i!!.addCategory(Intent.CATEGORY_LAUNCHER)
            startActivity(i)
        }
    }
}