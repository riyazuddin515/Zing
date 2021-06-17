package com.riyazuddin.zing.repositories.implementation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.entities.UserStat
import com.riyazuddin.zing.other.Constants.CHATS_COLLECTION
import com.riyazuddin.zing.other.Constants.CHAT_MESSAGE_PAGE_LIMIT
import com.riyazuddin.zing.other.Constants.DATE
import com.riyazuddin.zing.other.Constants.LAST_MESSAGE_PAGE_LIMIT
import com.riyazuddin.zing.other.Constants.MESSAGE
import com.riyazuddin.zing.other.Constants.MESSAGES_COLLECTION
import com.riyazuddin.zing.other.Constants.NO_MORE_MESSAGES
import com.riyazuddin.zing.other.Constants.SEEN
import com.riyazuddin.zing.other.Constants.SENDER_AND_RECEIVER_UID
import com.riyazuddin.zing.other.Constants.SENDING
import com.riyazuddin.zing.other.Constants.STATUS
import com.riyazuddin.zing.other.Constants.SUCCESS
import com.riyazuddin.zing.other.Constants.TYPE
import com.riyazuddin.zing.other.Constants.URL
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_STAT_COLLECTION
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val usersCollection = firestore.collection(USERS_COLLECTION)
    private val chatsCollection = firestore.collection(CHATS_COLLECTION)
    private val usersStatCollection = firestore.collection(USERS_STAT_COLLECTION)
    var playTone = MutableLiveData<Event<Resource<Boolean>>>()

    private var chatListLocalToRepo = mutableListOf<Message>()
    val chatList = MutableLiveData<Event<Resource<List<Message>>>>()

    private var isFirstPageFirstLoad = true
    private var lastVisible: DocumentSnapshot? = null
    private var firstListener: ListenerRegistration? = null
    private var nextListener: ListenerRegistration? = null

    private var lastMessageFirstListenerRegistration: ListenerRegistration? = null
    private var lastMessageNextListenerRegistration: ListenerRegistration? = null
    private var isLastMessageFirstLoad = true
    private var lastMessageLocalRepo = mutableListOf<LastMessage>()
    val lastMessageList = MutableLiveData<Event<Resource<List<LastMessage>>>>()
    private var lastMessageLastVisible: DocumentSnapshot? = null

    var unSeenLastMessagesCount = MutableLiveData<Event<Resource<Int>>>()
    var isUserOnline = MutableLiveData<Event<Resource<UserStat>>>()

    override suspend fun sendMessage(
        currentUid: String,
        receiverUid: String,
        message: String,
        type: String,
        uri: Uri?
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
            val messageOb = Message(
                messageId = messageId,
                message = message,
                type = type,
                date = System.currentTimeMillis(),
                senderAndReceiverUid = listOf(currentUid, receiverUid),
                url = messageMediaUrl ?: ""
            )
            chatsCollection.document(chatThread).collection(MESSAGES_COLLECTION).document(messageId)
                .set(messageOb).await()

            val lastMessage = LastMessage(message = messageOb, chatThread = chatThread)
            chatsCollection.document(chatThread).set(lastMessage).await()

            Resource.Success(messageOb)
        }
    }

    override suspend fun updateMessageStatusAsSeen(message: Message): Resource<String> =
        withContext(Dispatchers.IO) {
            safeCall {
                val chatThread =
                    getChatThread(message.senderAndReceiverUid[0], message.senderAndReceiverUid[1])
                chatsCollection.document(chatThread).collection(MESSAGES_COLLECTION)
                    .document(message.messageId).update(STATUS, SEEN).await()
                firestore.runTransaction { transition ->
                    val lastMessage = transition.get(chatsCollection.document(chatThread))
                        .toObject(LastMessage::class.java) ?: return@runTransaction
                    if (lastMessage.message.messageId == message.messageId) {
                        transition.update(
                            chatsCollection.document(chatThread),
                            "$MESSAGE.$STATUS",
                            SEEN
                        )
                    }
                }.await()
                Resource.Success(SUCCESS)
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
                .collection(MESSAGES_COLLECTION)
                .orderBy(DATE, Query.Direction.DESCENDING)
                .limit(CHAT_MESSAGE_PAGE_LIMIT)

            firstListener =
                firstQuery.addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
                    error?.let {
                        Log.e(TAG, "getChatLoadFirstQuery: ", it)
                        chatList.postValue(Event(Resource.Error(it.message!!)))
                    }
                    value?.let { querySnapshot ->
                        when {
                            querySnapshot.size() <= 0 -> {
                                chatList.postValue(Event(Resource.Success(listOf())))
                                return@addSnapshotListener
                            }
                            else -> {
                                lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                            }
                        }
//                        if (isFirstPageFirstLoad) {
//                            if (querySnapshot.size() - 1 < 0) {
//                                chatList.postValue(Event(Resource.Success(listOf())))
//                                return@addSnapshotListener
//                            } else {
//                                lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
//                                Log.i(TAG, "getChatLoadFirstQuery: last ${lastVisible.toString()}")
//                            }
//                        }

                        for (doc in querySnapshot.documentChanges) {
                            val message = doc.document.toObject(Message::class.java)
                            when (doc.type) {
                                DocumentChange.Type.ADDED -> {
                                    val bool = doc.document.metadata.hasPendingWrites()
                                    if (bool) {
                                        message.status = SENDING
                                    }
                                    Log.e(TAG, "loadFirstQuery: ${message.message}")
                                    if (isFirstPageFirstLoad) {
                                        chatListLocalToRepo.add(message)
                                    } else {
                                        chatListLocalToRepo.add(0, message)
                                        if (message.senderAndReceiverUid[0] != currentUid && !isFirstPageFirstLoad)
                                            playTone.postValue(Event(Resource.Success(true)))
                                    }
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    chatListLocalToRepo[doc.newIndex] = message
                                    Log.i(TAG, "getChatLoadFirstQuery: Modified")
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
                .collection(MESSAGES_COLLECTION)
                .orderBy(DATE, Query.Direction.DESCENDING)
                .startAfter(lastVisible ?: return@withContext)
                .limit(CHAT_MESSAGE_PAGE_LIMIT)

            nextListener = nextQuery.addSnapshotListener { value, error ->
                error?.let {
                    Log.e(TAG, "loadMorePost: ", it)
                    chatList.postValue(Event(Resource.Error(it.message!!)))
                    return@addSnapshotListener
                }
                value?.let { querySnapshot ->
                    when {
                        querySnapshot.size() - 1 < 0 -> {
                            Log.i(TAG, "getChatLoadMore: query less than zero")
                            chatList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                            return@addSnapshotListener
                        }
                        lastVisible == querySnapshot.documents[querySnapshot.size() - 1] -> {
                            Log.i(TAG, "getChatLoadMore: same as previous")
                            nextListener?.remove()
                            chatList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
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
                                        chatListLocalToRepo.indexOf(
                                            index
                                        )
                                    }"
                                )
                                chatListLocalToRepo[chatListLocalToRepo.indexOf(index)] = message
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

    override suspend fun getLastMessageFirstQuery(currentUser: User) {
        withContext(Dispatchers.IO) {
            val query = chatsCollection
                .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
                .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                .limit(LAST_MESSAGE_PAGE_LIMIT)

            lastMessageFirstListenerRegistration = query.addSnapshotListener { value, error ->
                error?.let {
                    Log.e(TAG, "getLastMessageFirstQuery: ", it)
                    lastMessageList.postValue(Event(Resource.Error(it.message!!)))
                    return@addSnapshotListener
                }
                value?.let { querySnapshot ->
                    when {
                        querySnapshot.size() <= 0 -> {
                            lastMessageList.postValue(Event(Resource.Success(listOf())))
                            return@addSnapshotListener
                        }
                        else -> {
                            lastMessageLastVisible =
                                querySnapshot.documents[querySnapshot.size() - 1]
                        }
                    }

                    for (doc in querySnapshot.documentChanges) {
                        val lastMessage = doc.document.toObject(LastMessage::class.java)

                        usersCollection.document(
                            if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid)
                                lastMessage.message.senderAndReceiverUid[1]
                            else
                                lastMessage.message.senderAndReceiverUid[0]

                        ).get().addOnSuccessListener {

                            val user = it.toObject(User::class.java)!!
                            if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid) {
                                lastMessage.receiver = user
                                lastMessage.sender = currentUser
                            } else {
                                lastMessage.receiver = currentUser
                                lastMessage.sender = user
                            }

                            Log.i(TAG, "getLastMessageFirstQuery: user -> ${user.name}")
                            Log.i(TAG, "getLastMessageFirstQuery: $lastMessage")

                            when (doc.type) {
                                DocumentChange.Type.ADDED -> {
                                    lastMessageLocalRepo.removeAll { existingLastMessage ->
                                        existingLastMessage.chatThread == lastMessage.chatThread
                                    }
                                    val bool = doc.document.metadata.hasPendingWrites()
                                    if (bool) {
                                        Log.i(TAG, "getLastMessageFirstQuery: hasPending")
                                        lastMessage.message.status = SENDING
                                    }
                                    if (isLastMessageFirstLoad) {
                                        lastMessageLocalRepo.add(lastMessage)
                                    } else {
                                        lastMessageLocalRepo.add(0, lastMessage)
                                    }
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    lastMessageLocalRepo.removeAll { existingLastMessage ->
                                        existingLastMessage.chatThread == lastMessage.chatThread
                                    }
                                    lastMessageLocalRepo.add(0, lastMessage)
                                }
                                else -> {
                                }
                            }
                            lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
                            isLastMessageFirstLoad = false
                        }
                    }
                }
            }
        }
    }

    override suspend fun getLastMessageLoadMore(currentUser: User) {
        withContext(Dispatchers.IO) {

            val nextQuery = chatsCollection
                .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
                .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                .startAfter(lastMessageLastVisible ?: return@withContext)
                .limit(LAST_MESSAGE_PAGE_LIMIT)


            lastMessageNextListenerRegistration = nextQuery.addSnapshotListener { value, error ->
                error?.let {
                    Log.e(TAG, "getLastMessageLoadMore: ", it)
                    lastMessageList.postValue(Event(Resource.Error(it.message!!)))
                    return@addSnapshotListener
                }

                value?.let { querySnapshot ->
                    when {
                        querySnapshot.size() <= 0 -> {
                            Log.i(TAG, "getLastMessageLoadMore: query less than zero")
                            lastMessageList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                            return@addSnapshotListener
                        }
                        lastMessageLastVisible == querySnapshot.documents[querySnapshot.size() - 1] -> {
                            Log.i(TAG, "getLastMessageLoadMore: same as previous")
                            lastMessageNextListenerRegistration?.remove()
                            lastMessageList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                            return@addSnapshotListener
                        }
                        else -> {
                            lastMessageLastVisible =
                                querySnapshot.documents[querySnapshot.size() - 1]
                        }
                    }

                    for (doc in querySnapshot.documentChanges) {
                        val lastMessage = doc.document.toObject(LastMessage::class.java)
                        usersCollection.document(
                            if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid)
                                lastMessage.message.senderAndReceiverUid[1]
                            else
                                lastMessage.message.senderAndReceiverUid[0]

                        ).get().addOnSuccessListener {
                            val user = it.toObject(User::class.java)!!
                            if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid) {
                                lastMessage.receiver = user
                                lastMessage.sender = currentUser
                            } else {
                                lastMessage.receiver = currentUser
                                lastMessage.sender = user
                            }

                            Log.i(TAG, "getLastMessageLoadMore: user -> ${user.name}")
                            Log.i(TAG, "getLastMessageLoadMore: $lastMessage")

                            when (doc.type) {
                                DocumentChange.Type.ADDED -> {
                                    lastMessageLocalRepo.removeAll { existingLastMessage ->
                                        existingLastMessage.chatThread == lastMessage.chatThread
                                    }
                                    lastMessageLocalRepo.add(lastMessage)
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    lastMessageLocalRepo.removeAll { existingLastMessage ->
                                        existingLastMessage.chatThread == lastMessage.chatThread
                                    }
                                    lastMessageLocalRepo.add(0, lastMessage)
                                }
                                else -> {

                                }
                            }
                            lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
                        }
                    }
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
            MESSAGE to "This message was Deleted",
            URL to "",
            TYPE to "DELETED"
        )

        chatsCollection
            .document(getChatThread(currentUid, otherEndUserUid))
            .collection(MESSAGES_COLLECTION)
            .document(message.messageId)
            .update(map.toMap()).await()

        Log.i(TAG, "deleteChatMessage: MessageDeleted")
    }

    override suspend fun getUser(uid: String): Resource<User> = withContext(Dispatchers.IO) {
        safeCall {
            val user = firestore.collection(USERS_COLLECTION).document(uid)
                .get().await().toObject(User::class.java)!!
            Resource.Success(user)
        }

    }

    override suspend fun getUnSeenLastMessagesCount(uid: String) {
        try {
            val query1 =
                chatsCollection
                    .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", uid)
                    .whereNotEqualTo("$MESSAGE.$STATUS", SEEN)
                    .limit(100)

            query1.addSnapshotListener { value, error ->
                error?.let {
                    return@addSnapshotListener
                }

                value?.let { querySnapshot ->
                    Log.i(TAG, "getUnSeenLastMessagesCount: invoked")
                    querySnapshot.toObjects(LastMessage::class.java).filter {
                        it.message.senderAndReceiverUid[1] == uid
                    }.let {
                        unSeenLastMessagesCount.postValue(Event(Resource.Success(it.size)))
                    }
                }
            }
        } catch (e: Exception) {
            unSeenLastMessagesCount.postValue(Event(Resource.Error(e.localizedMessage ?: "")))
        }

    }

    override suspend fun checkUserIsOnline(uid: String) {
        val query = usersStatCollection.document(uid)
        query.addSnapshotListener { value, error ->
            error?.let {
                return@addSnapshotListener
            }
            value?.let {
                val userStat = it.toObject(UserStat::class.java) ?: return@let
                isUserOnline.postValue(Event(Resource.Success(userStat)))
            }
        }
    }

    companion object {
        const val TAG = "DefaultChatRepo"
    }

    private fun getChatThread(currentUid: String, otherEndUserUid: String) =
        if (currentUid < otherEndUserUid)
            currentUid + otherEndUserUid
        else
            otherEndUserUid + currentUid
}