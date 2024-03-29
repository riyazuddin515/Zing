package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Followers(
    val followedByUid: String = "",
    @ServerTimestamp
    val since: Date? = null
)