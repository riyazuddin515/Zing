package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.riyazuddin.zing.other.Constants.DEFAULT_PROFILE_PICTURE_URL

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val username: String = "",
    val profilePic: String = DEFAULT_PROFILE_PICTURE_URL,
    val isEmailVerified: Boolean = false,
    val follows: List<String> = listOf(),
    val bio: String = "",
    @get:Exclude
    val isFollowing: Boolean = false,
)