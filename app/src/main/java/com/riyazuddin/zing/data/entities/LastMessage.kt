package com.riyazuddin.zing.data.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity(tableName = "lastMessages")
data class LastMessage(
    @Embedded
    val message: Message = Message(),
    @PrimaryKey
    val chatThread: String = "",
    val receiverUid: String = "",

    @get:Exclude
    var otherUser: User = User()

)