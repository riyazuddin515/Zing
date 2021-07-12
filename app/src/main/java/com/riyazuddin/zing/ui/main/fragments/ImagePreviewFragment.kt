package com.riyazuddin.zing.ui.main.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentImagePreviewBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImagePreviewFragment : Fragment(R.layout.fragment_image_preview) {

    private val viewModel by viewModels<ChatViewModel>()
    private val args: ImagePreviewFragmentArgs by navArgs()
    private lateinit var binding: FragmentImagePreviewBinding

    private lateinit var uri: Uri

    private lateinit var resultLauncher: ActivityResultLauncher<Any>
    private val contract = object : ActivityResultContract<Any, Uri?>() {
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity(args.stringUri.toUri())
                .setActivityTitle(getString(R.string.crop_image))
                .setGuidelines(CropImageView.Guidelines.ON)
                .getIntent(requireContext())
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent).uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentImagePreviewBinding.inflate(layoutInflater)
        uri = args.stringUri.toUri()
        resultLauncher = registerForActivityResult(contract) {
            it?.let {
                uri = it
                binding.ivPreview.setImageURI(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clickListeners()
        binding.ivPreview.setImageURI(args.stringUri.toUri())

    }

    private fun clickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.ivCrop.setOnClickListener {
            resultLauncher.launch(Any())
        }
        binding.btnSend.setOnClickListener {
            viewModel.sendMessage(
                currentUid = Firebase.auth.uid!!,
                receiverUid = args.otherEndUserUid,
                message = binding.TIEMessage.text.toString(),
                type = Constants.IMAGE,
                uri = uri,
                args.replyToMessageId
            )
            Toast.makeText(requireContext(), getString(R.string.sending), Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }
}