package com.riyazuddin.zing.repositories.implementation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.pagingsource.FollowingAndFollowersPagingSource
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Singleton

@Singleton
class DefaultChatRepository : ChatRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection(Constants.CHATS_COLLECTION)

    private var chatListLocalToRepo = mutableListOf<Message>()
    val chatList = MutableLiveData<Event<Resource<List<Message>>>>()

    private var isFirstPageFirstLoad = true
    private lateinit var lastVisible: DocumentSnapshot
    private lateinit var firstListener: ListenerRegistration
    private lateinit var nextListener: ListenerRegistration

    override suspend fun getFollowersAndFollowingForNewChat(uid: String): Resource<Pager<QuerySnapshot, User>> {
        return Resource.Success(
            Pager(PagingConfig(Constants.NEW_CHAT_LIST_SIZE)) {
                FollowingAndFollowersPagingSource(uid)
            }
        )
    }

    override suspend fun sendMessage(
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
    ): Resource<Message> = withContext(Dispatchers.IO) {
        safeCall {
            val chatThread = getChatThread(currentUid, receiverUid)
            val messageId = UUID.randomUUID().toString()

            val messageMediaUrl = uri?.let {
                Log.e("TAG", "sendMessage: Uri not null")
                FirebaseStorage.getInstance().reference
                    .child("chatMedia/$chatThread/$messageId").putFile(it)
                    .await().metadata?.reference?.downloadUrl?.await().toString()
            }


            Log.e("TAG", "sendMessage: stored $messageMediaUrl")

            val messageOb = Message(
                messageId = messageId,
                message = message,
                type = type,
                date = System.currentTimeMillis(),
                senderAddReceiverUid = listOf(currentUid, receiverUid),
                url = messageMediaUrl ?: ""
            )

            Log.e("TAG", "sendMessage: message created")
            //upload message and lastMessage
            chatsCollection
                .document(chatThread)
                .collection(Constants.MESSAGES_COLLECTION)
                .document(messageId)
                .set(messageOb)
                .await()

            Log.e("TAG", "sendMessage: message posted")

            val lastMessage = LastMessage(
                message = messageOb,

                senderName = senderName,
                senderUserName = senderUsername,
                senderProfilePicUrl = senderProfilePicUrl,

                receiverName = receiverName,
                receiverUsername = receiverUsername,
                receiverProfilePicUrl = receiveProfileUrl
            )

            chatsCollection.document(chatThread).set(lastMessage).await()

            Log.e("TAG", "sendMessage: last message updated")

            Resource.Success(messageOb)
        }
    }

    override fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String) {
        val firstQuery = FirebaseFirestore.getInstance()
            .collection("chats")
            .document(getChatThread(currentUid, otherEndUserUid))
            .collection("messages")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(20)

        firstListener = firstQuery.addSnapshotListener { value, error ->
            value?.let {
                // please add if doc not empty
                if (isFirstPageFirstLoad) {
                    if (it.size() - 1 < 0) {
                        return@addSnapshotListener
                    } else {
                        lastVisible = it.documents[it.size() - 1] // array 0, 1, 2
                    }
                }

                for (doc in it.documentChanges) {
                    when (doc.type) {
                        DocumentChange.Type.ADDED -> {
                            val message = doc.document.toObject(Message::class.java)
                            Log.e(TAG, "loadFirstQuery: ${message.message}")
                            if (isFirstPageFirstLoad) {
                                chatListLocalToRepo.add(message)
                            } else {
                                chatListLocalToRepo.add(0, message)
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val message = doc.document.toObject(Message::class.java)
                            Log.w(TAG, "loadFirstQuery: DeletedMessage ---> ${message.message}")
                            chatListLocalToRepo[doc.newIndex] = message
                        }
                        else -> {
                        }
                    }
                }
                chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
                isFirstPageFirstLoad = false
            }
        }
    }

    // Method to load more post
    override fun getChatLoadMore(currentUid: String, otherEndUserUid: String) {

        val nextQuery = FirebaseFirestore.getInstance()
            .collection("chats")
            .document(getChatThread(currentUid, otherEndUserUid))
            .collection("messages")
            .orderBy("date", Query.Direction.DESCENDING)
            .startAfter(lastVisible)
            .limit(10)

        nextListener = nextQuery.addSnapshotListener { value, error ->
            error?.let {
                Log.e(TAG, "loadMorePost: ", it)
                return@addSnapshotListener
            }
            value?.let {
                if (it.size() - 1 < 0) {
                    return@addSnapshotListener
                } else {
                    lastVisible = it.documents[it.size() - 1]
                }

                for (doc in it.documentChanges) {
                    when (doc.type) {
                        DocumentChange.Type.ADDED -> {
                            chatListLocalToRepo.add(doc.document.toObject(Message::class.java))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val message = doc.document.toObject(Message::class.java)
                            val index = chatListLocalToRepo.filter { m ->
                                m.messageId == message.messageId
                            }[0]
                            Log.w(
                                TAG,
                                "loadFirstQuery: DeletedMessage ---> ${
                                    chatListLocalToRepo.indexOf(index)
                                }"
                            )
                            chatListLocalToRepo[chatListLocalToRepo.indexOf(index)] = message
//                            deleteIndex.postValue(doc.newIndex)
                        }
                        else -> {
                        }
                    }
                }
                chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
            }
        }
    }

    override fun clearChatList() {
        isFirstPageFirstLoad = true
        firstListener.remove()
        nextListener.remove()
        chatListLocalToRepo.clear()
        chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
    }

    override suspend fun deleteChatMessage(currentUid: String, otherEndUserUid: String, message: Message){

        val map = mutableMapOf(
            "message" to "This message was Deleted",
            "url" to "",
            "type" to "DELETED"
        )

        chatsCollection
            .document(getChatThread(currentUid, otherEndUserUid))
            .collection("messages")
            .document(message.messageId)
            .update(map.toMap()).await()

        Log.i(TAG, "deleteChatMessage: MessageDeleted")
    }

    private fun getChatThread(currentUid: String, otherEndUserUid: String) =
        if (currentUid < otherEndUserUid)
            currentUid + otherEndUserUid
        else
            otherEndUserUid + currentUid

    companion object {
        const val TAG = "DefaultChatRepo"
    }

}