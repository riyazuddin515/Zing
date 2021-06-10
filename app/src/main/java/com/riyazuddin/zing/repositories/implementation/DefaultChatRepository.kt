package com.riyazuddin.zing.repositories.implementation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.entities.UserStat
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.DATE
import com.riyazuddin.zing.other.Constants.MESSAGE
import com.riyazuddin.zing.other.Constants.MESSAGES_COLLECTION
import com.riyazuddin.zing.other.Constants.NO_MORE_MESSAGES
import com.riyazuddin.zing.other.Constants.RECEIVER_NAME
import com.riyazuddin.zing.other.Constants.RECEIVER_PROFILE_PIC_URL
import com.riyazuddin.zing.other.Constants.RECEIVER_USERNAME
import com.riyazuddin.zing.other.Constants.SEEN
import com.riyazuddin.zing.other.Constants.SENDER_AND_RECEIVER_UID
import com.riyazuddin.zing.other.Constants.SENDER_NAME
import com.riyazuddin.zing.other.Constants.SENDER_PROFILE_PIC_URL
import com.riyazuddin.zing.other.Constants.SENDER_USERNAME
import com.riyazuddin.zing.other.Constants.SENDING
import com.riyazuddin.zing.other.Constants.STATUS
import com.riyazuddin.zing.other.Constants.SUCCESS
import com.riyazuddin.zing.other.Constants.TYPE
import com.riyazuddin.zing.other.Constants.URL
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.pagingsource.FollowingAndFollowersPagingSource
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.internal.wait
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val usersCollection = firestore.collection(Constants.USERS_COLLECTION)
    private val chatsCollection = firestore.collection(Constants.CHATS_COLLECTION)
    private val usersStatCollection = firestore.collection(Constants.USERS_STAT_COLLECTION)
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
                senderAndReceiverUid = listOf(currentUid, receiverUid),
                url = messageMediaUrl ?: ""
            )

            Log.e("TAG", "sendMessage: message created")
            //upload message and lastMessage

            chatsCollection
                .document(chatThread)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
                .set(messageOb)
                .await()

            Log.e("TAG", "sendMessage: message posted")

            val lastMessage = LastMessage(message = messageOb, chatThread = chatThread)


            val ex = chatsCollection.document(chatThread).get().await()
            if (ex.exists()) {
                Log.i(TAG, "sendMessage: updated")
                val map = mapOf(
                    MESSAGE to messageOb,

                    SENDER_NAME to senderName,
                    SENDER_USERNAME to senderUsername,
                    SENDER_PROFILE_PIC_URL to senderProfilePicUrl,

                    RECEIVER_NAME to receiverName,
                    RECEIVER_USERNAME to receiverUsername,
                    RECEIVER_PROFILE_PIC_URL to receiveProfileUrl
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

    override suspend fun updateMessageStatusAsSeen(message: Message): Resource<String> =
        withContext(Dispatchers.IO) {
            safeCall {

                firestore.runTransaction { transition ->

                    val lastMessageRef = chatsCollection
                        .document(
                            getChatThread(
                                message.senderAndReceiverUid[0],
                                message.senderAndReceiverUid[1]
                            )
                        )

                    val c = transition.get(lastMessageRef)
                    val id = c.toObject(LastMessage::class.java)!!.message.messageId
                    if (id == message.messageId) {
                        transition.update(lastMessageRef, "$MESSAGE.$STATUS", SEEN)
                        Log.i(TAG, "updateMessageStatusAsSeen: lastMessage status seen updated")
                    }
                    transition.update(
                        lastMessageRef.collection(MESSAGES_COLLECTION).document(message.messageId),
                        STATUS, SEEN
                    )
                    Log.i(TAG, "updateMessageStatusAsSeen: message status seen updated")
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
                .collection("messages")
                .orderBy("date", Query.Direction.DESCENDING)

            firstListener =
                firstQuery.limit(20).addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->

                    error?.let {
                        Log.e(TAG, "getChatLoadFirstQuery: ", it)
                        chatList.postValue(Event(Resource.Error(it.message!!)))
                    }
                    value?.let { querySnapshot ->
                        if (isFirstPageFirstLoad) {
                            if (querySnapshot.size() - 1 < 0) {
                                chatList.postValue(Event(Resource.Success(listOf())))
                                return@addSnapshotListener
                            } else {
                                lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                                Log.i(TAG, "getChatLoadFirstQuery: last ${lastVisible.toString()}")
                            }
                        }

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
                .startAfter(lastVisible?.get(DATE))
                .limit(10)

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
            val query =
                chatsCollection.whereArrayContains(
                    "$MESSAGE.$SENDER_AND_RECEIVER_UID",
                    Firebase.auth.uid!!
                )
                    .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                    .limit(3)

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

                        if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid) {
                            lastMessage.sender = currentUser
                        } else {
                            lastMessage.receiver = currentUser
                        }

                        usersCollection.document(
                            if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid)
                                lastMessage.message.senderAndReceiverUid[1]
                            else
                                lastMessage.message.senderAndReceiverUid[0]

                        ).get().addOnSuccessListener {

                            if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid) {
                                lastMessage.receiver = it.toObject(User::class.java)!!
                            } else {
                                lastMessage.sender = currentUser
                            }

                            lastMessage.sender = it.toObject(User::class.java)!!

                            Log.i(TAG, "getLastMessageFirstQuery: $lastMessage")

                            when (doc.type) {
                                DocumentChange.Type.ADDED -> {

                                    Log.i(TAG, "getLastMessageFirstQuery: <-----ADDED INVOKED------>")
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
                                    Log.i(TAG, "getLastMessageFirstQuery: LastMessageModified")
                                    val list = lastMessageLocalRepo.filter { lastMessagePara ->
                                        lastMessagePara.chatThread == lastMessage.chatThread
                                    }
                                    if (list.isNotEmpty()) {
                                        val index = lastMessageLocalRepo.indexOf(list[0])
                                        lastMessageLocalRepo.removeAt(index)
                                        lastMessageLocalRepo.add(0, lastMessage)
                                    }
                                }
                                else -> {
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
        }
    }

    override suspend fun getLastMessageLoadMore() {
        withContext(Dispatchers.IO) {

            val nextQuery = chatsCollection
                .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
                .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                .startAfter(lastMessageLastVisible?.get("$MESSAGE.$DATE"))
                .limit(3)


            lastMessageNextListenerRegistration = nextQuery.addSnapshotListener { value, error ->
                error?.let {
                    Log.e(TAG, "getLastMessageLoadMore: ", it)
                    lastMessageList.postValue(Event(Resource.Error(it.message!!)))
                    return@addSnapshotListener
                }

                value?.let { querySnapshot ->
                    Log.i(
                        TAG, "getLastMessageLoadMore: Last Visible = ${
                            lastMessageLastVisible?.get(
                                RECEIVER_NAME
                            )
                        }"
                    )
                    Log.i(TAG, "getLastMessageLoadMore: size = ${querySnapshot.size()}")
                    for (doc in querySnapshot.documents) {
                        val lastMessage = doc.toObject(LastMessage::class.java)
                    }
                    when {
                        querySnapshot.size() - 1 < 0 -> {
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

                    querySnapshot.documents.forEach {
                        it.toObject(LastMessage::class.java)?.let { it1 ->
                            lastMessageLocalRepo.add(
                                it1
                            )
                        }
                    }
                    lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
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
                chatsCollection.whereArrayContains(
                    "$MESSAGE.$SENDER_AND_RECEIVER_UID",
                    uid
                ).whereNotEqualTo("$MESSAGE.$STATUS", SEEN)
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
            print(e.localizedMessage)
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