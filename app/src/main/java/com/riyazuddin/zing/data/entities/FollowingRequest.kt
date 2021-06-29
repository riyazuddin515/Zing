package com.riyazuddin.zing.data.entities

/**
 * uids in the list are those uids who wants to
 * follow current user
 */
data class FollowingRequest(
    val requestedToUids: List<String> = listOf(),
    val uid: String = ""
)