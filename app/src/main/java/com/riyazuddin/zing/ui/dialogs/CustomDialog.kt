package com.riyazuddin.zing.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Message
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.riyazuddin.zing.R

class CustomDialog(
    private val title: String,
    private val message: String
): DialogFragment() {

    private var positiveListener: (() -> Unit)? = null
    fun setPositiveListener(listener: () -> Unit){
        positiveListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.delete_post_dialog_positive){ _,_ ->
                positiveListener?.let { click ->
                    click()
                }
            }
            .setNegativeButton(R.string.delete_post_dialog_negative){ dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
    }
}