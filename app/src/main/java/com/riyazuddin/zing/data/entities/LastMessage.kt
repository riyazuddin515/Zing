package com.riyazuddin.zing.data.entities

import com.google.firebase.firestore.Exclude

data class LastMessage(
    val message: Message = Message(),
    val chatThread: String = "",

    @get:Exclude
    var sender: User = User(),
    @get:Exclude
    var receiver: User = User()

)