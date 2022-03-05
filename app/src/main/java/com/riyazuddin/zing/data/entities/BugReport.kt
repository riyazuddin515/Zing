package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class BugReport(
    val title: String,
    val description: String,
    val uid: String,
    @ServerTimestamp
    val date: Date? = null,
)
