package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Comment(
    val commentId: String = "",
    val comment: String = "",
    val postId: String = "",
    val commentedBy: String = "",
    @ServerTimestamp
    val date: Date? = null,
    @get:Exclude var username: String? = null,
    @get:Exclude var userProfilePic: String? = null
)