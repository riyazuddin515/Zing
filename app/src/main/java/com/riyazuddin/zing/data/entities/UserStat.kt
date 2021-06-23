package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class UserStat(
    @ServerTimestamp
    val lastSeen: Date? = null,
    val state: String = "",
    val token: String = ""
)