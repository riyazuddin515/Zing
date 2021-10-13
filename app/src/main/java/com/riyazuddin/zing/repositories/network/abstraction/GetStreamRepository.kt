package com.riyazuddin.zing.repositories.network.abstraction

import com.riyazuddin.zing.other.Resource
import io.getstream.chat.android.client.models.User

interface GetStreamRepository {

    suspend fun getToken(userId: String): Resource<String>

    suspend fun connectUser(user: User): Resource<User>

    suspend fun createChatChannel(currentUid: String, otherEndUserUid: String): Resource<String>

    suspend fun setFCMTokenInStream(): Resource<Unit>

}