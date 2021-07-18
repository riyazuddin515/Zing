package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Feed(
    val postId: String = "",
    @ServerTimestamp
    val date: Date? = null
)