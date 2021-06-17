package com.riyazuddin.zing.data.entities

import com.riyazuddin.zing.other.Constants.DEFAULT_PROFILE_PICTURE_URL

data class UpdateProfile(
    val uidToUpdate: String = "",
    var profilePicUrl: String = DEFAULT_PROFILE_PICTURE_URL,
    val name: String = "",
    val username: String = "",
    val bio: String = ""
)
