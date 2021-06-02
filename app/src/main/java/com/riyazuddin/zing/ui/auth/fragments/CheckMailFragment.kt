package com.riyazuddin.zing.ui.auth.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCheckMailBinding
import com.riyazuddin.zing.other.snackBar
import java.util.*


class CheckMailFragment : Fragment(R.layout.fragment_check_mail) {

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
            i?.let {
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                startActivity(i)
            } ?: snackBar("Can't able to open Gmail. Open Manually")

        }
    }
}