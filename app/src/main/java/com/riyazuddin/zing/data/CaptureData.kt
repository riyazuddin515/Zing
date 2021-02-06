package com.riyazuddin.zing.data

import android.net.Uri
import java.io.Serializable

data class CaptureData(
    val imageUri: Uri? = null
) : Serializable