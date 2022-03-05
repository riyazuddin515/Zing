package com.riyazuddin.zing.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentBugReportBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.main.viewmodels.BugReportViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BugReportFragment : Fragment(R.layout.fragment_bug_report) {

    private lateinit var binding: FragmentBugReportBinding
    private val viewModel: BugReportViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentBugReportBinding.bind(view)

        subscribeToObservers()
        setUpClickListeners()
    }

    private fun subscribeToObservers() {
        viewModel.bugReportStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackBar(it)
                onErrorOrSuccess(false)
            },
            onLoading = {
                onLoading()
            }
        ) {
            if (it) {
                snackBar(getString(R.string.bug_reported))
            }
            onErrorOrSuccess(true)
        })
    }

    private fun setUpClickListeners() {
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnSubmit.setOnClickListener {
            binding.btnSubmit.isVisible = false
            viewModel.submitBug(
                binding.TIETitle.text?.trim().toString(),
                binding.TIEDescription.text?.trim().toString()
            )
        }
    }

    private fun onLoading() {
        binding.btnSubmit.isVisible = false
        binding.progressCircular.isVisible = true
    }

    private fun onErrorOrSuccess(isSuccess: Boolean) {
        binding.progressCircular.isVisible = true
        binding.btnSubmit.isVisible = true
        if (isSuccess) {
            binding.TIETitle.text?.clear()
            binding.TIEDescription.text?.clear()
        }
    }

}