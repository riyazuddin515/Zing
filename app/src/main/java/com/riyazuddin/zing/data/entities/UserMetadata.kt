package com.riyazuddin.zing.data.entities

data class UserMetadata(
    var uid: String = "",
    var followingCount: Int = 0,
    var followersCount: Int = 0,
    var postCount: Int = 0
)