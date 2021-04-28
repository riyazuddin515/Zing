package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.riyazuddin.zing.other.Constants.SENT

@IgnoreExtraProperties
data class Message(
    val messageId: String = "",
    var message: String = "",
    val url: String = "",
    val type: String = "",
    val date: Long = 0L,
    val senderAddReceiverUid: List<String> = listOf(),
    var status: String = SENT,
    @get:Exclude
    var userProfileImage: String = "",
    @get:Exclude
    var name: String = ""
)