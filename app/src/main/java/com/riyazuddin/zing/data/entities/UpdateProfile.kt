package com.riyazuddin.zing.data.entities

data class UpdateProfile(
    val uidToUpdate: String = "",
    var profilePicUrl: String = "",
    val name: String = "",
    val username: String = "",
    val bio: String = ""
)
