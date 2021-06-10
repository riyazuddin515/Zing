package com.riyazuddin.zing.data.entities

data class PostLikes(
    var likedBy: List<String> = listOf(),
    var uid: String = ""
)