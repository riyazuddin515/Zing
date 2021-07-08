package com.riyazuddin.zing.repositories.network.implementation

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
import com.riyazuddin.zing.other.Constants.DELETED
import com.riyazuddin.zing.other.Constants.FAILURE
import com.riyazuddin.zing.other.Constants.MESSAGE
import com.riyazuddin.zing.other.Constants.MESSAGES_COLLECTION
import com.riyazuddin.zing.other.Constants.NO_MORE_MESSAGES
import com.riyazuddin.zing.other.Constants.RECEIVER_UID
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
import com.riyazuddin.zing.repositories.local.LastMessageDao
import com.riyazuddin.zing.repositories.network.abstraction.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val lastMessageDao: LastMessageDao
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
    private var noMoreChatMessages = false

    private var lastMessageListener: ListenerRegistration? = null
    private var lastMessageLastVisible: DocumentSnapshot? = null

    val haveUnSeenMessages = MutableLiveData<Event<Resource<Boolean>>>()
    private var unSeenMessagesListener: ListenerRegistration? = null

    var isUserOnline = MutableLiveData<Event<Resource<UserStat>>>()
    private var checkOnlineListener: ListenerRegistration? = null

    companion object {
        const val TAG = "DefaultChatRepo"
    }

    private fun getChatThread(currentUid: String, otherEndUserUid: String) =
        if (currentUid < otherEndUserUid)
            currentUid + otherEndUserUid
        else
            otherEndUserUid + currentUid

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
                senderAndReceiverUid = listOf(currentUid, receiverUid),
                url = messageMediaUrl ?: ""
            )
            chatsCollection.document(chatThread).collection(MESSAGES_COLLECTION).document(messageId)
                .set(messageOb).await()

            val lastMessage =
                LastMessage(message = messageOb, chatThread = chatThread, receiverUid = receiverUid)
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
                        GlobalScope.launch {
                            lastMessageDao.updateLastMessageAsSeen(chatThread, SEEN)
                        }
                    }
                }.await()
                Resource.Success(SUCCESS)
            }
        }

    override suspend fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String) {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "getChatLoadFirstQuery: Invoked")
            val firstQuery = chatsCollection
                .document(getChatThread(currentUid, otherEndUserUid))
                .collection(MESSAGES_COLLECTION)
                .orderBy(DATE, Query.Direction.DESCENDING)
                .limit(CHAT_MESSAGE_PAGE_LIMIT)

            firstListener = firstQuery.addSnapshotListener { value, error ->
                error?.let {
                    Log.e(TAG, "getChatLoadFirstQuery: ", it)
                    chatList.postValue(Event(Resource.Error(it.message!!)))
                }
                value?.let { querySnapshot ->
                    when {
                        querySnapshot.isEmpty -> {
                            chatList.postValue(Event(Resource.Success(listOf())))
                            return@addSnapshotListener
                        }
                        isFirstPageFirstLoad -> {
                            lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                        }
                    }
                    for (doc in querySnapshot.documentChanges) {
                        val message = doc.document.toObject(Message::class.java)
                        when (doc.type) {
                            DocumentChange.Type.ADDED -> {
                                if (doc.document.metadata.hasPendingWrites()) {
                                    message.status = SENDING
                                    message.date = Date()
                                }

                                if (isFirstPageFirstLoad)
                                    chatListLocalToRepo.add(message)
                                else
                                    chatListLocalToRepo.add(0, message)
                            }
                            DocumentChange.Type.MODIFIED -> {
                                chatListLocalToRepo[doc.newIndex] = message
                            }
                            else -> {
                            }
                        }
                    }
                    isFirstPageFirstLoad = false
                    if (chatListLocalToRepo.isNotEmpty()) {
                        val lastMessage = chatListLocalToRepo[0]
                        if (lastMessage.senderAndReceiverUid[0] != currentUid && lastMessage.status != SEEN)
                            playTone.postValue(Event(Resource.Success(true)))
                    }
                    chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
                }
            }
            Log.i(TAG, "getChatLoadFirstQuery: Completed")
        }
    }

    override suspend fun getChatLoadMore(currentUid: String, otherEndUserUid: String) {
        withContext(Dispatchers.IO) {
            safeCall {
                Log.i(TAG, "getChatLoadMore: Invoked")
                if (noMoreChatMessages) {
                    chatList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                    return@withContext
                }
                Log.i(TAG, "getChatLoadMore: Retrieving more")
                val querySnapshot = chatsCollection
                    .document(getChatThread(currentUid, otherEndUserUid))
                    .collection(MESSAGES_COLLECTION)
                    .orderBy(DATE, Query.Direction.DESCENDING)
                    .startAfter(lastVisible ?: return@withContext)
                    .limit(CHAT_MESSAGE_PAGE_LIMIT)
                    .get().await()

                if (querySnapshot.isEmpty || lastVisible == querySnapshot.documents[querySnapshot.size() - 1]) {
                    noMoreChatMessages = true
                    chatList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                    return@withContext
                }
                lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                val list = querySnapshot.toObjects(Message::class.java)
                chatListLocalToRepo.addAll(list)
                chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
                Log.i(TAG, "getChatLoadMore: Completed")
                Resource.Success(true) //No Use
            }

        }
    }

    override fun clearChatList() {
        isFirstPageFirstLoad = true
        noMoreChatMessages = false
        firstListener?.remove()
        lastVisible = null
        playTone.postValue(Event(Resource.Success(false)))
        chatListLocalToRepo.clear()
        chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
    }

    override suspend fun lastMessageListener(currentUser: User) {
        val querySnapshot = chatsCollection
            .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
            .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
            .limit(1)

        lastMessageListener = querySnapshot
            .addSnapshotListener { value, error ->
                error?.let {
                    return@addSnapshotListener
                }
                value?.let {
                    if (it.isEmpty) {
                        return@addSnapshotListener
                    }
                    Log.i(TAG, "lastMessageListener: invoked")
                    for (e in it.documentChanges) {
                        val lastMessage = e.document.toObject(LastMessage::class.java)
                        GlobalScope.launch {
                            val a =
                                lastMessageDao.checkChatThreadAlreadyExists(lastMessage.chatThread)
                            if (a == 0) {
                                lastMessageDao.insertLastMessage(
                                    getOtherUserForRecentChat(
                                        lastMessage
                                    )
                                )
                            } else {
                                lastMessage.otherUser =
                                    lastMessageDao.getLastMessage(lastMessage.chatThread).otherUser
                                lastMessageDao.insertLastMessage(lastMessage)
                            }
                        }
                    }
                }
            }

    }

    override fun removeLastMessageListener() {
        lastMessageListener?.remove()
    }

    override suspend fun deleteChatMessage(
        currentUid: String,
        otherEndUserUid: String,
        message: Message
    ) {
        withContext(Dispatchers.IO) {
            safeCall {
                val map = mutableMapOf(
                    MESSAGE to "This message was Deleted",
                    URL to "",
                    TYPE to DELETED
                )

                chatsCollection
                    .document(getChatThread(currentUid, otherEndUserUid))
                    .collection(MESSAGES_COLLECTION)
                    .document(message.messageId)
                    .update(map.toMap()).await()

                message.apply {
                    this.message = "This message was Deleted"
                    url = ""
                    type = DELETED
                }

                val list = chatListLocalToRepo.filter {
                    it.messageId == message.messageId
                }
                if (list.isNotEmpty()) {
                    for (e in list) {
                        val index = chatListLocalToRepo.indexOf(e)
                        chatListLocalToRepo.removeAt(index)
                        chatListLocalToRepo.add(index, message)
                    }
                    chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
                }

                Resource.Success(message)
            }
        }
    }

    override suspend fun getUser(uid: String): Resource<User> = withContext(Dispatchers.IO) {
        safeCall {
            val user = firestore.collection(USERS_COLLECTION).document(uid)
                .get().await().toObject(User::class.java)!!
            Resource.Success(user)
        }

    }

    override suspend fun checkUserIsOnline(uid: String) {
        val query = usersStatCollection.document(uid)
        checkOnlineListener = query.addSnapshotListener { value, error ->
            error?.let {
                return@addSnapshotListener
            }
            value?.let {
                val userStat = it.toObject(UserStat::class.java) ?: return@let
                isUserOnline.postValue(Event(Resource.Success(userStat)))
            }
        }
    }

    override suspend fun checkForUnSeenMessage(uid: String) {
        withContext(Dispatchers.IO) {
            try {
                val query = chatsCollection
                    .whereEqualTo(RECEIVER_UID, uid)
                    .whereNotEqualTo("$MESSAGE.$STATUS", SEEN)
                    .orderBy("$MESSAGE.$STATUS", Query.Direction.DESCENDING)
                    .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                    .limit(1)

                unSeenMessagesListener = query.addSnapshotListener { value, error ->
                    error?.let {
                        Log.e(TAG, "checkForUnSeenMessage: ", it)
                        haveUnSeenMessages.postValue(
                            Event(
                                Resource.Error(
                                    it.localizedMessage ?: FAILURE
                                )
                            )
                        )
                        return@addSnapshotListener
                    }
                    value?.let { querySnapshot ->
                        if (querySnapshot.isEmpty) {
                            haveUnSeenMessages.postValue(Event(Resource.Success(false)))
                        } else {
                            haveUnSeenMessages.postValue(Event(Resource.Success(true)))
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "checkForUnSeenMessage: ", e)
                haveUnSeenMessages.postValue(
                    Event(
                        Resource.Error(
                            e.localizedMessage ?: "Can't able to check unSeenMessage"
                        )
                    )
                )
            }
        }
    }

    private suspend fun getOtherUserForRecentChat(lastMessage: LastMessage): LastMessage = run {
        val otherUserUid = if (Firebase.auth.uid!! == lastMessage.message.senderAndReceiverUid[0])
            lastMessage.message.senderAndReceiverUid[1]
        else
            lastMessage.message.senderAndReceiverUid[0]

        val doc = usersCollection.document(otherUserUid).get().await()
        lastMessage.otherUser = doc.toObject(User::class.java)!!

        lastMessage
    }

    override suspend fun removeUnSeenMessageListener() {
        unSeenMessagesListener?.remove()
    }

    override suspend fun removeCheckOnlineListener() {
        checkOnlineListener?.remove()
    }

    override fun getLastMessagesFromRoom() = lastMessageDao.getAllLastMessages()

    override suspend fun getLastMessages(): Resource<Boolean> = withContext(Dispatchers.IO) {
        safeCall {
            val last = lastMessageDao.getLastLastMessage()

            if (last == null) {
                val querySnapshot = chatsCollection
                    .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
                    .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                    .get().await()
                if (!querySnapshot.isEmpty) {
                    val list = mutableListOf<LastMessage>()
                    querySnapshot.forEach {
                        val l = it.toObject(LastMessage::class.java)
                        list.add(getOtherUserForRecentChat(l))
                        Log.e(TAG, "getLastMessages IF: list -> ${l.otherUser.name}")
                    }
                    lastMessageDao.insertLastMessages(list)
                }
            } else {
                val querySnapshot = chatsCollection
                    .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
                    .whereGreaterThan("$MESSAGE.$DATE", last.message.date!!)
                    .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                    .get().await()
                if (!querySnapshot.isEmpty) {
                    val list = mutableListOf<LastMessage>()
                    querySnapshot.forEach {
                        val l = it.toObject(LastMessage::class.java)
                        list.add(getOtherUserForRecentChat(l))
                        Log.e(TAG, "getLastMessages ELSE: list -> ${l.otherUser.name}")
                    }
                    lastMessageDao.insertLastMessages(list)
                } else
                    Log.d(TAG, "getLastMessages: empty")
            }

            Resource.Success(true)
        }
    }

    override suspend fun syncLastMessagesOtherUserData(chatThread: String, uid: String) {
        withContext(Dispatchers.IO) {
            safeCall {
                val user = usersCollection.document(uid).get().await().toObject(User::class.java)!!
                val lastMessage = lastMessageDao.getLastMessage(chatThread)
                if (user.name != lastMessage.otherUser.name || user.profilePicUrl != lastMessage.otherUser.profilePicUrl || user.username != lastMessage.otherUser.username) {
                    lastMessage.otherUser.name = user.name
                    lastMessage.otherUser.profilePicUrl = user.profilePicUrl
                    lastMessage.otherUser.username = user.username
                    lastMessageDao.insertLastMessage(lastMessage)
                }
                Resource.Success(true)
            }
        }
    }
}