package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Message(
    val messageId: String = "",
    val message: String = "",
    val url: String = "",
    val type: String = "",
    val date: Long = 0L,
    val senderAddReceiverUid: List<String> = listOf(),
    @get:Exclude
    var userProfileImage: String = "",
    @get:Exclude
    var name: String = ""
)