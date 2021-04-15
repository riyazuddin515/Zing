package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude

data class Post(
    val postId: String = "",
    val postedBy: String = "",
    val date: Long = 0L,
    val imageUrl: String = "",
    val caption: String = "",
    var likeCount: Int = 0,
    @get:Exclude var username: String? = null,
    @get:Exclude var userProfilePic: String? = null,
    @get:Exclude var isLiking: Boolean = false,
    @get:Exclude var isLiked: Boolean = false
)