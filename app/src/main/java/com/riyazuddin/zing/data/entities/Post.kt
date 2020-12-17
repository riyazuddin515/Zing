package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude

data class Post(
    val postId: String? = null,
    val uid: String? = null,
    val date: Long = 0L,
    val imageUrl: String? = null,
    val caption: String? = null,
    @get:Exclude var username: String? = null,
    @get:Exclude var userProfilePic: String? = null,
    @get:Exclude var isLiking: Boolean = false,
    @get:Exclude var isLiked: Boolean = false
)