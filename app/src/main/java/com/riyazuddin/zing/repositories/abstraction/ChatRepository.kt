package com.riyazuddin.zing.repositories.abstraction

import android.net.Uri
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Resource

interface ChatRepository {

    suspend fun sendMessage(
        currentUid: String,
        receiverUid: String,
        message: String,
        type: String,
        uri: Uri?
    ): Resource<Message>

    fun updateChatListOnMessageSent(message: Message)

    suspend fun updateMessageStatusAsSeen(message: Message): Resource<String>

    suspend fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String)

    suspend fun getChatLoadMore(currentUid: String, otherEndUserUid: String)

    fun clearChatList()

    fun clearRecentMessagesList()

    suspend fun getLastMessageFirstQuery(currentUser: User)

    suspend fun getLastMessageLoadMore(currentUser: User)

    suspend fun deleteChatMessage(currentUid: String, otherEndUserUid: String, message: Message)

    suspend fun getUser(uid: String): Resource<User>

    suspend fun getUnSeenLastMessagesCount(uid: String)

    suspend fun checkUserIsOnline(uid: String)
}