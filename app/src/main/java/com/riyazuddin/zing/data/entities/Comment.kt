package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude

data class Comment(
    val commentId: String = "",
    val comment: String = "",
    val postId: String = "",
    val date: Long = 0L,
    val commentedBy: String = "",
    var isLike: Boolean = false,
    @get:Exclude var isLiking: Boolean = false,
    @get:Exclude var username: String? = null,
    @get:Exclude var userProfilePic: String? = null,
)