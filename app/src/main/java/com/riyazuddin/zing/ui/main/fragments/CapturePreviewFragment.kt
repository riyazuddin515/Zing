package com.riyazuddin.zing.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCapturePreviewBinding
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