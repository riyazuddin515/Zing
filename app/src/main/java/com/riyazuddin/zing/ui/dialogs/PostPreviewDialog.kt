package com.riyazuddin.zing.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Message
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.ItemGridPostPreviewBinding

class PostPreviewDialog(private val itemView: View): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(itemView)
            .create()
    }
}