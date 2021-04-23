package com.riyazuddin.zing.data.entities

data class LastMessage(
    val message: Message,

    var senderName: String = "",
    var senderUserName: String = "",
    var senderProfilePicUrl: String = "",

    var receiverName: String = "",
    var receiverUsername: String = "",
    var receiverProfilePicUrl: String = ""

)