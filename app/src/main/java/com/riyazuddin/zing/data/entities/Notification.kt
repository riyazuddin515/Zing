package com.riyazuddin.zing.data.entities

import java.io.Serializable

data class Notification(
    val title: String,
    val tag: String,
    var image: String,
    var body: String,
    val sound: String,
    val notification_channel_id: String,
    val cid: String
) : Serializable