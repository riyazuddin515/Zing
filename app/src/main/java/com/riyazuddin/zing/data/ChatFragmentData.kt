package com.riyazuddin.zing.data

import java.io.Serializable

data class ChatFragmentData(
    var uid: String = "",
    var name: String = "",
    var username: String = "",
    var profilePicUrl: String = ""
): Serializable

