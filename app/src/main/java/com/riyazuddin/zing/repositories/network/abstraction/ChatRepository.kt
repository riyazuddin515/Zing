package com.riyazuddin.zing.repositories.network.abstraction

import android.net.Uri
import androidx.lifecycle.LiveData
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Resource

interface ChatRepository {

    suspend fun sendMessage(
        currentUid: String,
        receiverUid: String,
        message: String,
        type: String,
        uri: Uri?,
        replyToMessageId: String?
    ): Resource<Message>

    suspend fun updateMessageStatusAsSeen(message: Message): Resource<String>

    suspend fun getChatLoadFirstQuery(
        currentUid: String,
        otherEndUserUid: String,
        otherEndUsername: String
    )

    suspend fun getChatLoadMore(
        currentUid: String,
        otherEndUserUid: String,
        otherEndUsername: String
    )

    fun clearChatList()

    suspend fun lastMessageListener(currentUser: User)

    suspend fun deleteChatMessage(currentUid: String, otherEndUserUid: String, message: Message)

    suspend fun getUser(uid: String): Resource<User>

    suspend fun checkUserIsOnline(uid: String)

    suspend fun checkForUnSeenMessage(uid: String)

    suspend fun removeUnSeenMessageListener()

    suspend fun removeCheckOnlineListener()

    /**
     * @since 4-7-2021
     * Remove the LastMessageListener
     */
    fun removeLastMessageListener()

    /**
     * @since 3-7-2021
     * @return LiveData<List<LastMessages>>
     */
    fun getLastMessagesFromRoom(): LiveData<List<LastMessage>>

    /**
     * @since 4-7-2021
     * @return Resource<Boolean>
     *     Boolean true indicates the load is successful
     *     and the LastMessage Listener can be attached
     */
    suspend fun getLastMessages(): Resource<Boolean>

    /**
     * @since 6-7-2021
     * @param chatThread to update the other user profile in DB
     * @param uid for loading lastMessage other user data
     */
    suspend fun syncLastMessagesOtherUserData(chatThread: String, uid: String)
}