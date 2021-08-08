package com.riyazuddin.zing.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentAccountPrivacyBottomSheetBinding
import com.riyazuddin.zing.other.Constants.PRIVATE

class AccountPrivacyBottomSheetFragment(
    private val privacy: String
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAccountPrivacyBottomSheetBinding

    private var clickListener: (() -> Unit)? =null
    fun onClickListener(listener: () -> Unit) {
        clickListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentAccountPrivacyBottomSheetBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (privacy == PRIVATE){
            binding.title.text = getString(R.string.switch_to_private_account)
            binding.materialTextView2.text = getString(R.string.only_your_followers_can_see_your_posts)
            binding.materialTextView3.text = getString(R.string.you_can_able_to_change_it_later)
            binding.btnSwitch.text = getString(R.string.switch_to_private)
        }else{
            binding.title.text = getString(R.string.switch_to_public_account)
            binding.materialTextView2.text = getString(R.string.everyone_can_able_to_see_your_posts)
            binding.materialTextView3.text = getString(R.string.you_can_able_to_change_it_later)
            binding.btnSwitch.text = getString(R.string.switch_to_public)
        }

        binding.btnSwitch.setOnClickListener {
            clickListener?.let {
                it()
            }
        }
    }
}