package com.riyazuddin.zing.ui.main.fragments.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.UpdateProfile
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentProfileInfoBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.NO_USER_DOCUMENT
import com.riyazuddin.zing.other.Constants.SEARCH_TIME_DELAY
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.Validator
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.SettingsViewModel
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileInfo : Fragment(R.layout.fragment_profile_info) {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var user: User
    private lateinit var validator: Validator

    private lateinit var binding: FragmentProfileInfoBinding
    private val viewModel: SettingsViewModel by viewModels()

    private var imageUri: Uri? = null
    private lateinit var cropContent: ActivityResultLauncher<String>
    private val cropImageActivityResultContract = object : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String?): Intent {
            return CropImage.activity()
                .setAspectRatio(1, 1)
                .setOutputCompressQuality(40)
                .setActivityTitle("Crop Image")
                .setGuidelines(CropImageView.Guidelines.ON)
                .getIntent(requireContext())
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        validator = Validator(requireContext())
        cropContent = registerForActivityResult(cropImageActivityResultContract) {
            it?.let {
                viewModel.setImage(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileInfoBinding.bind(view)

        subscribeToObservers()

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.getUserProfile(Firebase.auth.uid!!)

        binding.tvChangeProfileImage.setOnClickListener {
            cropContent.launch("image/*")
        }

        var job: Job? = null
        binding.TIEUsername.addTextChangedListener { editable ->
            binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
            binding.TILUsername.error = null
            job?.cancel()
            job = lifecycleScope.launch {
                delay(SEARCH_TIME_DELAY)
                editable?.let {
                    if (it.toString() != user.username) {
                        validator.validateUsername(it.toString()).apply {
                            if (this != Constants.VALID) {
                                binding.TILUsername.error = this
                            } else {
                                viewModel.checkUserNameAvailability(it.toString())
                            }
                        }
                    }
                }
            }
        }

        binding.TIEName.addTextChangedListener {
            validator.validateName(it.toString()).apply {
                if (this != Constants.VALID)
                    binding.TILName.error = this
                else
                    binding.TILName.error = null
            }
        }

        binding.btnUpdate.setOnClickListener {

            val name = binding.TIEName.text.toString().trim()
            val username = binding.TIEUsername.text.toString().trim()

            validator.validateName(name).apply {
                if (this != Constants.VALID) {
                    binding.TILName.error = this
                    return@setOnClickListener
                }
            }

            if (username != user.username) {
                validator.validateUsername(username).apply {
                    if (this != Constants.VALID) {
                        binding.TILUsername.error = this
                        return@setOnClickListener
                    }
                }
            }

            it.isVisible = false
            val updateProfile = UpdateProfile(
                uidToUpdate = Firebase.auth.uid!!,
                name = binding.TIEName.text.toString(),
                username = binding.TIEUsername.text.toString(),
                bio = binding.TIEBio.text.toString(),
                profilePicUrl = user.profilePicUrl
            )
            viewModel.updateProfile(updateProfile, imageUri)
        }

    }

    private fun subscribeToObservers() {
        viewModel.imageUri.observe(viewLifecycleOwner) {
            imageUri = it
            glide.load(it).into(binding.CIVProfilePic)
        }

        viewModel.userProfileStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                if (it == NO_USER_DOCUMENT){
                    user = User()
                    glide.load(R.drawable.default_profile_pic).into(binding.CIVProfilePic)
                }
                else
                    snackBar(it)
                binding.progressBar.isVisible = false
            },
            onLoading = { binding.progressBar.isVisible = true }
        ) { user ->
            this.user = user
            binding.progressBar.isVisible = false
            glide.load(user.profilePicUrl).into(binding.CIVProfilePic)
            binding.TIEName.setText(user.name)
            binding.TIEUsername.setText(user.username)
            binding.TIEBio.setText(user.bio)
        })

        viewModel.updateProfileStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                binding.progressBar.isVisible = false
                binding.btnUpdate.isVisible = true
            },
            onLoading = { binding.progressBar.isVisible = true }
        ) {
            binding.progressBar.isVisible = false
            binding.btnUpdate.isVisible = true
            snackBar("Profile successfully updated")
        })

        viewModel.isUsernameAvailable.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
                binding.TILUsername.error = it
            },
            onLoading = {
                binding.TILUsername.error = null
                binding.TILUsername.endIconMode = TextInputLayout.END_ICON_NONE
            }
        ) {
            binding.TILUsername.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.TILUsername.endIconDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_outline_check_circle, null)
        })
    }
}