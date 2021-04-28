package com.riyazuddin.zing.repositories.implementation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.pagingsource.FollowingAndFollowersPagingSource
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.NO_MORE_MESSAGES
import com.riyazuddin.zing.other.Constants.PENDING
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Singleton


@Singleton
class DefaultChatRepository : ChatRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection(Constants.CHATS_COLLECTION)

    var playTone = MutableLiveData<Event<Resource<Boolean>>>()

    private var chatListLocalToRepo = mutableListOf<Message>()
    val chatList = MutableLiveData<Event<Resource<List<Message>>>>()

    private var isFirstPageFirstLoad = true
    private var lastVisible: DocumentSnapshot? = null
    private var firstListener: ListenerRegistration? = null
    private var nextListener: ListenerRegistration? = null

    private var lastMessageFirstListenerRegistration: ListenerRegistration? = null
    private var isLastMessageFirstLoad = true
    private var lastMessageLocalRepo = mutableListOf<LastMessage>()
    val lastMessageList = MutableLiveData<Event<Resource<List<LastMessage>>>>()
    private var lastMessageLastVisible: DocumentSnapshot? = null

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


            val ex = chatsCollection.document(chatThread).get().await()
            if (ex.exists()) {
                Log.i(TAG, "sendMessage: updated")
                val map = mapOf(
                    "message" to messageOb,

                    "senderName" to senderName,
                    "senderUserName" to senderUsername,
                    "senderProfilePicUrl" to senderProfilePicUrl,

                    "receiverName" to receiverName,
                    "receiverUsername" to receiverUsername,
                    "receiverProfilePicUrl" to receiveProfileUrl
                )
                chatsCollection.document(chatThread).update(map.toMap()).await()
            } else {
                Log.i(TAG, "sendMessage: set")
                chatsCollection.document(chatThread).set(lastMessage).await()
            }

            Log.e("TAG", "sendMessage: last message updated")

            Resource.Success(messageOb)
        }
    }

    override fun updateChatListOnMessageSent(message: Message) {
        val m = chatListLocalToRepo.filter {
            it.messageId == message.messageId
        }[0]
        val index = chatListLocalToRepo.indexOf(m)
        chatListLocalToRepo[index] = message
        chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
    }

    override suspend fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String) {
        withContext(Dispatchers.IO) {
            val firstQuery = chatsCollection
                .document(getChatThread(currentUid, otherEndUserUid))
                .collection("messages")
                .orderBy("date", Query.Direction.DESCENDING)

            firstListener = firstQuery.limit(20).addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->

                error?.let {
                    Log.e(TAG, "getChatLoadFirstQuery: ", it)
                    chatList.postValue(Event(Resource.Error(it.message!!)))
                }
                value?.let { querySnapshot ->
                    if (isFirstPageFirstLoad) {
                        if (querySnapshot.size() - 1 < 0) {
                            return@addSnapshotListener
                        } else {
                            lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                            Log.i(TAG, "getChatLoadFirstQuery: last ${lastVisible.toString()}")
                        }
                    }

                    for (doc in querySnapshot.documentChanges) {
                        when (doc.type) {
                            DocumentChange.Type.ADDED -> {
                                val message = doc.document.toObject(Message::class.java)
                                val bool = doc.document.metadata.hasPendingWrites()
                                if (bool) {
                                    message.status = PENDING
                                }
                                Log.e(TAG, "loadFirstQuery: ${message.message}")
                                if (isFirstPageFirstLoad) {
                                    chatListLocalToRepo.add(message)
                                } else {
                                    chatListLocalToRepo.add(0, message)
                                    playTone.postValue(Event(Resource.Success(true)))
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                val message = doc.document.toObject(Message::class.java)
                                Log.w(TAG, "loadFirstQuery: DeletedMessage ---> ${message.message}")
                                chatListLocalToRepo[doc.newIndex] = message
                                Log.i(TAG, "getChatLoadFirstQuery: Modifi")
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
    }

    // Method to load more post
    override suspend fun getChatLoadMore(currentUid: String, otherEndUserUid: String) {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "getChatLoadMore: inside function")
            val nextQuery = chatsCollection
                .document(getChatThread(currentUid, otherEndUserUid))
                .collection("messages")
                .orderBy("date", Query.Direction.DESCENDING)
                .startAfter(lastVisible?.get("date"))
                .limit(10)

//            val l = nextQuery.get().await().toObjects(Message::class.java)
//            for (e in l) {
//                Log.d(TAG, "getChatLoadMore: ${e.message} --- ${e.messageId}")
//            }
            nextListener = nextQuery.addSnapshotListener { value, error ->
                error?.let {
                    Log.e(TAG, "loadMorePost: ", it)
                    chatList.postValue(Event(Resource.Error(it.message!!)))
                    return@addSnapshotListener
                }
                value?.let { querySnapshot ->
                    for (doc in querySnapshot.documentChanges) {
                        val message = doc.document.toObject(Message::class.java)
                        Log.d(TAG, "getChatLoadMore: ${message.messageId} ----- ${message.message}")
                    }
                    when {
                        querySnapshot.size() - 1 < 0 -> {
                            Log.i(TAG, "getChatLoadMore: query less than zero")
                            return@addSnapshotListener
                        }
                        lastVisible == querySnapshot.documents[querySnapshot.size() - 1] -> {
                            Log.i(TAG, "getChatLoadMore: same as previous")
                            nextListener?.remove()
                            lastMessageList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                            return@addSnapshotListener
                        }
                        else -> {
                            lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                        }
                    }

                    for (doc in querySnapshot.documentChanges) {
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
    }

    override fun clearChatList() {
        isFirstPageFirstLoad = true
        firstListener?.remove()
        nextListener?.remove()
        chatListLocalToRepo.clear()
        chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
    }

    override fun clearRecentMessagesList() {
        isLastMessageFirstLoad = true
        lastMessageFirstListenerRegistration?.remove()
        lastMessageLocalRepo.clear()
        lastMessageList.postValue(Event((Resource.Success(lastMessageLocalRepo))))
    }

    override suspend fun getLastMessageFirstQuery(currentUid: String) {
        withContext(Dispatchers.IO) {
            val query =
                chatsCollection.whereArrayContains("message.senderAddReceiverUid", currentUid)
                    .orderBy("message.date", Query.Direction.DESCENDING)
                    .limit(10)

            lastMessageFirstListenerRegistration = query.addSnapshotListener { value, error ->
                error?.let {
                    Log.e(TAG, "getLastMessageFirstQuery: ", it)
                    lastMessageList.postValue(Event(Resource.Error(it.message!!)))
                    return@addSnapshotListener
                }
                value?.let { querySnapshot ->
                    if (isLastMessageFirstLoad) {
                        if (querySnapshot.size() - 1 < 0) {
                            return@addSnapshotListener
                        } else {
                            lastMessageLastVisible =
                                querySnapshot.documents[querySnapshot.size() - 1]
                        }
                    }
                    for (doc in querySnapshot.documentChanges) {
                        val lastMessage = doc.document.toObject(LastMessage::class.java)

                        Log.i(TAG, "getLastMessageFirstQuery: $lastMessage")

                        when (doc.type) {
                            DocumentChange.Type.ADDED -> {
                                Log.i(TAG, "getLastMessageFirstQuery: <-----ADDED INVOKED------>")
                                if (isLastMessageFirstLoad) {
                                    lastMessageLocalRepo.add(lastMessage)
                                } else {
                                    lastMessageLocalRepo.add(0, lastMessage)
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                val oldIndex = doc.oldIndex
                                val newIndex = doc.newIndex
                                Log.i(
                                    TAG,
                                    "getLastMessageFirstQuery: oldIndex = $oldIndex ---- newIndex = $newIndex"
                                )
                                val list = lastMessageLocalRepo.filter {
                                    if (it.message.senderAddReceiverUid[0] == currentUid) {
                                        it.receiverUsername == lastMessage.receiverUsername
                                    } else {
                                        it.senderUserName == lastMessage.senderUserName
                                    }
                                }
                                if (list.isNotEmpty()) {
                                    val index = lastMessageLocalRepo.indexOf(list[0])
                                    lastMessageLocalRepo.removeAt(index)
                                    lastMessageLocalRepo.add(0, lastMessage)
                                }
//                                if (list.isEmpty()) {
//                                    lastMessageLocalRepo.add(0, lastMessage)
//                                } else {
//                                    val index = lastMessageLocalRepo.indexOf(list[0])
//                                    lastMessageLocalRepo.removeAt(index)
//                                    lastMessageLocalRepo.add(0, lastMessage)
//                                }
                            }
                            else -> {
                            }
                        }
                    }
                    for (e in lastMessageLocalRepo) {
                        Log.w(
                            TAG,
                            "getLastMessageFirstQuery: ${e.message.messageId} ----- ${e.message.message}"
                        )
                    }
                    lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
                    isLastMessageFirstLoad = false
                }
            }
        }
    }

    override suspend fun deleteChatMessage(
        currentUid: String,
        otherEndUserUid: String,
        message: Message
    ) {

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

    override suspend fun getUser(uid: String): Resource<User> = withContext(Dispatchers.IO) {
        safeCall {
            val user = firestore.collection(USERS_COLLECTION).document(uid)
                .get().await().toObject(User::class.java)!!
            Resource.Success(user)
        }

    }
}