package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Post(
    val postId: String = "",
    val postedBy: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    var likeCount: Int = 0,
    @ServerTimestamp
    val date: Date? = null,
    @get:Exclude var username: String? = null,
    @get:Exclude var userProfilePic: String? = null,
    @get:Exclude var isLiking: Boolean = false,
    @get:Exclude var isLiked: Boolean = false
)