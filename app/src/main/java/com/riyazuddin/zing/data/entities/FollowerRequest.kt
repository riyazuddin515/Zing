package com.riyazuddin.zing.data.entities

data class FollowerRequest(
    val requestedUids: List<String> = listOf(),
    val uid: String = ""
)