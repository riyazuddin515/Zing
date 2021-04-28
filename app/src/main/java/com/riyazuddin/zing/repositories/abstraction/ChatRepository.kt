package com.riyazuddin.zing.repositories.abstraction

import android.net.Uri
import androidx.paging.Pager
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Resource

interface ChatRepository {

    suspend fun getFollowersAndFollowingForNewChat(uid: String): Resource<Pager<QuerySnapshot, User>>

    suspend fun sendMessage(
        currentUid: String,
        receiverUid: String,
        message: String,
        type: String,
        uri: Uri?,
        senderName: String,
        senderUsername: String,
        senderProfilePicUrl: String,
        receiverName: String,
        receiverUsername: String,
        receiveProfileUrl: String
    ): Resource<Message>

    fun updateChatListOnMessageSent(message: Message)

    suspend fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String)

    suspend fun getChatLoadMore(currentUid: String, otherEndUserUid: String)

    fun clearChatList()

    fun clearRecentMessagesList()

    suspend fun getLastMessageFirstQuery(currentUid: String)

    suspend fun deleteChatMessage(currentUid: String, otherEndUserUid: String, message: Message)

    suspend fun getUser(uid: String): Resource<User>
}