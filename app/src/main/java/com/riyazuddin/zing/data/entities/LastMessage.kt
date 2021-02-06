package com.riyazuddin.zing.data.entities

data class LastMessage(
    val messageId: String = "",
    val message: String = "",
    val url: String = "",
    val type: String = "",
    val date: Long = 0L,
    val senderAddReceiverUid: List<String> = listOf(),

    var senderName: String = "",
    var senderUserName: String = "",
    var senderProfilePicUrl: String = "",

    var receiverName: String = "",
    var receiverUsername: String = "",
    var receiverProfilePicUrl: String = ""

)