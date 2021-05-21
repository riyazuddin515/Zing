package com.riyazuddin.zing.data

data class Notification(
    val body: String,
    val channel_id: String,
    val image: String,
    val notification_priority: String,
    val sound: String,
    val tag: String,
    val title: String
)