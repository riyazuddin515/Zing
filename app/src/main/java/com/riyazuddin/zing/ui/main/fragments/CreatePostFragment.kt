package com.riyazuddin.zing.ui.main.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentCreatePostBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.slideUpViews
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.CreatePostViewModel
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    @Inject
    lateinit var glide: RequestManager
    private lateinit var binding: FragmentCreatePostBinding
    private val viewModel: CreatePostViewModel by viewModels()
    private lateinit var cropContent: ActivityResultLauncher<String>
    private var currentImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        viewModel.setImageUri(it)
    }

    private val cropContact = object : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String?): Intent {
            return CropImage.activity(viewModel.currentImageUri.value)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setOutputCompressQuality(50)
                .setActivityTitle(getString(R.string.crop_image))
                .getIntent(requireContext())
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cropContent = registerForActivityResult(cropContact) {
            it?.let {
                viewModel.setCroppedImageUri(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreatePostBinding.bind(view)

        subscribeToObservers()

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnSelectImage.setOnClickListener {
            pickImage.launch("image/*")
        }
        binding.IMVCreatePost.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnPost.setOnClickListener {
            currentImageUri?.let { uri ->
                binding.btnPost.isEnabled = false
                binding.btnPost.isClickable = false
                viewModel.createPost(uri, binding.TIECaption.text.toString())
            } ?: snackBar(resources.getString(R.string.error_select_an_image))
        }

        slideUpViews(
            requireContext(),
            binding.IMVCreatePost,
            binding.btnSelectImage,
            binding.TILCaption,
            binding.btnPost
        )
    }

    private fun subscribeToObservers() {
        viewModel.currentImageUri.observe(viewLifecycleOwner) {
            cropContent.launch(null)
        }

        viewModel.croppedImageUri.observe(viewLifecycleOwner) {
            currentImageUri = it
            binding.btnSelectImage.isVisible = false
            glide.load(it).into(binding.IMVCreatePost)
        }

        viewModel.createPostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.btnPost.isEnabled = true
                binding.btnPost.isClickable = true
                binding.progressBar.isVisible = false
                snackBar(it)
            },
            onLoading = { binding.progressBar.isVisible = true }
        ) {
            binding.progressBar.isVisible = false
            findNavController().popBackStack()
        })
    }
}