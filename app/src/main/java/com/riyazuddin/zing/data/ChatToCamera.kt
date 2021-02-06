package com.riyazuddin.zing.data

import android.net.Uri
import java.io.Serializable

data class ChatToCamera(
    val isForChat: Boolean = false,
    val currentUid: String = "",
    val receiver: String = "",
    val type: String = "",
    var uri: Uri? = null,

    var senderName: String = "",
    var senderUsername: String = "",
    var senderProfilePicUrl: String = "",

    var receiverName: String = "",
    var receiverUsername: String = "",
    var receiverProfilePicUrl: String = ""

): Serializable