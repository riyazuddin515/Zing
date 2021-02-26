package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCapturePreviewBinding
import com.riyazuddin.zing.other.Constants.IMAGE
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CapturePreviewFragment : Fragment(R.layout.fragment_capture_preview) {

    private lateinit var binding: FragmentCapturePreviewBinding

    @Inject
    lateinit var glide: RequestManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCapturePreviewBinding.bind(view)


    }


    companion object {
        const val TAG = "PreviewFragmentLog"
    }

}