package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.riyazuddin.zing.other.Constants.SENT
import java.util.*

@IgnoreExtraProperties
data class Message(
    val messageId: String = "",
    var message: String = "",
    var url: String = "",
    var type: String = "",
    @ServerTimestamp
    var date: Date? = null,
    val senderAndReceiverUid: List<String> = listOf(),
    var status: String = SENT,
    @get:Exclude
    var userProfileImage: String = "",
    @get:Exclude
    var name: String = ""
)