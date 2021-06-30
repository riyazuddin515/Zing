package com.riyazuddin.zing.data.entities

data class FollowingRequest(
    val requestedToUids: List<String> = listOf(),
    val uid: String = ""
)