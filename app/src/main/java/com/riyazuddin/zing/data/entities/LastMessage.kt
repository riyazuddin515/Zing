package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude

data class LastMessage(
    val message: Message = Message(),

    var senderName: String = "",
    var senderUserName: String = "",
    var senderProfilePicUrl: String = "",

    var receiverName: String = "",
    var receiverUsername: String = "",
    var receiverProfilePicUrl: String = "",

    @get:Exclude
    var user: User = User()

)