package com.riyazuddin.zing.data.entities

data class UserStat(
    val lastSeen: Long = 0L,
    val state: String = "",
    val token: String = ""
)