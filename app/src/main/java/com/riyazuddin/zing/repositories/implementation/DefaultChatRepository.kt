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
import com.riyazuddin.zing.other.Constants.FAILURE
import com.riyazuddin.zing.other.Constants.LAST_MESSAGE_PAGE_LIMIT
import com.riyazuddin.zing.other.Constants.MESSAGE
import com.riyazuddin.zing.other.Constants.MESSAGES_COLLECTION
import com.riyazuddin.zing.other.Constants.NO_MORE_MESSAGES
import com.riyazuddin.zing.other.Constants.RECEIVER_UID
import com.riyazuddin.zing.other.Constants.SEEN
import com.riyazuddin.zing.other.Constants.SENDER_AND_RECEIVER_UID
import com.riyazuddin.zing.other.Constants.SENDING
import com.riyazuddin.zing.other.Constants.SENT
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

    private var lastMessageListener: ListenerRegistration? = null
    private var lastMessageLocalRepo = mutableListOf<LastMessage>()
    val lastMessageList = MutableLiveData<Event<Resource<List<LastMessage>>>>()
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
                    }
                }.await()
                Resource.Success(SUCCESS)
            }
        }

    override fun updateChatListOnMessageSent(message: Message) {
//        val m = chatListLocalToRepo.filter {
//            it.messageId == message.messageId
//        }[0]
//        val index = chatListLocalToRepo.indexOf(m)
//        chatListLocalToRepo[index] = message
//        chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
    }

    override suspend fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String) {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "getChatLoadFirstQuery: Invoked")
            val firstQuery = chatsCollection
                .document(getChatThread(currentUid, otherEndUserUid))
                .collection(MESSAGES_COLLECTION)
                .orderBy(DATE, Query.Direction.DESCENDING)
                .limit(CHAT_MESSAGE_PAGE_LIMIT + 3)

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
                        else -> {
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
                    chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
                    isFirstPageFirstLoad = false
                    if (chatListLocalToRepo.isNotEmpty()){
                        val lastMessage = chatListLocalToRepo[0]
                        if (lastMessage.senderAndReceiverUid[0] != currentUid && lastMessage.status != SEEN)
                            playTone.postValue(Event(Resource.Success(true)))
                    }

                }
            }
            Log.i(TAG, "getChatLoadFirstQuery: Completed")
        }
    }

    override suspend fun getChatLoadMore(currentUid: String, otherEndUserUid: String) {
        withContext(Dispatchers.IO) {
            safeCall {
                Log.i(TAG, "getChatLoadMore: Invoked")
                val querySnapshot = chatsCollection
                    .document(getChatThread(currentUid, otherEndUserUid))
                    .collection(MESSAGES_COLLECTION)
                    .orderBy(DATE, Query.Direction.DESCENDING)
                    .startAfter(lastVisible ?: return@withContext)
                    .limit(CHAT_MESSAGE_PAGE_LIMIT)
                    .get().await()

                if (querySnapshot.isEmpty || lastVisible == querySnapshot.documents[querySnapshot.size() - 1]) {
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
        firstListener?.remove()
        lastVisible = null
        playTone.postValue(Event(Resource.Success(false)))
        chatListLocalToRepo.clear()
        chatList.postValue(Event(Resource.Success(chatListLocalToRepo)))
    }

    override fun clearRecentMessagesList() {
        lastMessageLastVisible = null
        lastMessageListener?.remove()
        lastMessageListener = null
        lastMessageLocalRepo.clear()
        lastMessageList.postValue(Event((Resource.Success(lastMessageLocalRepo))))
    }

    override suspend fun lastMessageListener(currentUser: User) {
        val querySnapshot = chatsCollection
            .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
            .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
            .limit(1)

        lastMessageListener = querySnapshot
            .addSnapshotListener { value, error ->
            error?.let {
                lastMessageList.postValue(Event(Resource.Error(it.message ?: FAILURE)))
                return@addSnapshotListener
            }
            value?.let {
                if (it.isEmpty) {
                    lastMessageList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                    return@addSnapshotListener
                }
                Log.i(TAG, "lastMessageListener: invoked")
                for (e in it.documentChanges) {
                    val lastMessage = e.document.toObject(LastMessage::class.java)
                    val list = lastMessageLocalRepo.filter { existingLastMessages ->
                        existingLastMessages.chatThread == lastMessage.chatThread
                    }
                    if (list.isEmpty()) {
                        GlobalScope.launch {
                            val l = getOtherUserForRecentChat(lastMessage, currentUser)
                            lastMessageLocalRepo.add(0, l)
                            lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
                        }
                    } else {
                        for (each in list) {
                            val index = lastMessageLocalRepo.indexOf(each)
                            val existing = lastMessageLocalRepo.removeAt(index)
                            if (lastMessage.message.senderAndReceiverUid[0] == existing.sender.uid) {
                                lastMessage.sender = existing.sender
                                lastMessage.receiver = existing.receiver
                            } else {
                                lastMessage.receiver = existing.sender
                                lastMessage.sender = existing.receiver
                            }

                            if (each.message != lastMessage.message) {
                                lastMessageLocalRepo.add(0, lastMessage)
                            } else {
                                lastMessageLocalRepo.add(index, lastMessage)

                            }
                            lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
                        }
                    }
                }
            }
        }
    }

    override suspend fun getLastMessageFirstQuery(currentUser: User): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            safeCall {
                lastMessageList.postValue(Event(Resource.Loading()))
                val querySnapshot = chatsCollection
                    .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
                    .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                    .limit(LAST_MESSAGE_PAGE_LIMIT)
                    .get().await()
                if (querySnapshot.documents.size <= 0) {
                    lastMessageList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                }else{
                    lastMessageLastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                    querySnapshot.forEach {
                        val documentSnapshot = getOtherUserForRecentChat(
                            it.toObject(LastMessage::class.java),
                            currentUser
                        )
                        Log.i(TAG, "getLastMessageFirstQuery: ${documentSnapshot.message.message}")
                        lastMessageLocalRepo.add(documentSnapshot)
                    }
                    lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
                }
                Resource.Success(true)
            }

        }

    override suspend fun getLastMessageLoadMore(currentUser: User) {
        withContext(Dispatchers.IO) {
            safeCall {
                val querySnapshot = chatsCollection
                    .whereArrayContains("$MESSAGE.$SENDER_AND_RECEIVER_UID", Firebase.auth.uid!!)
                    .orderBy("$MESSAGE.$DATE", Query.Direction.DESCENDING)
                    .startAfter(lastMessageLastVisible ?: return@withContext)
                    .limit(LAST_MESSAGE_PAGE_LIMIT)
                    .get().await()
                if (querySnapshot.isEmpty || lastMessageLastVisible == querySnapshot.documents[querySnapshot.size() - 1]) {
                    lastMessageList.postValue(Event(Resource.Error(NO_MORE_MESSAGES)))
                    return@withContext
                }
                lastMessageLastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                querySnapshot.forEach {
                    val documentSnapshot = getOtherUserForRecentChat(
                        it.toObject(LastMessage::class.java),
                        currentUser
                    )
                    lastMessageLocalRepo.add(documentSnapshot)
                }
                lastMessageList.postValue(Event(Resource.Success(lastMessageLocalRepo)))
                Resource.Success(true) // No use
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

    private suspend fun getOtherUserForRecentChat(
        lastMessage: LastMessage,
        currentUser: User
    ): LastMessage = run {
        val docId = if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid)
            lastMessage.message.senderAndReceiverUid[1]
        else
            lastMessage.message.senderAndReceiverUid[0]

        val it = usersCollection.document(docId).get().await()

        val user = it.toObject(User::class.java)!!
        if (lastMessage.message.senderAndReceiverUid[0] == currentUser.uid) {
            lastMessage.receiver = user
            lastMessage.sender = currentUser
        } else {
            lastMessage.receiver = currentUser
            lastMessage.sender = user
        }

        lastMessage
    }

    override fun removeUnSeenMessageListener() {
        unSeenMessagesListener?.remove()
    }

    override fun removeCheckOnlineListener() {
        checkOnlineListener?.remove()
    }
}