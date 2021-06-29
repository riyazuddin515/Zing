package com.riyazuddin.zing.data.entities

/**
 * uids in the list are those uids whom the
 * current user sendFollowingRequest
 */
data class FollowerRequest(
    val requestedUids: List<String> = listOf(),
    val uid: String = ""
)