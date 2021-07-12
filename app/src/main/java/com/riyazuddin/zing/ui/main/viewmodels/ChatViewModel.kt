package com.riyazuddin.zing.ui.main.viewmodels

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.entities.UserStat
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.network.implementation.DefaultChatRepository
import com.riyazuddin.zing.repositories.network.pagingsource.FollowingAndFollowersPagingSource
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import javax.inject.Singleton

@ActivityScoped
@Singleton
class ChatViewModel @ViewModelInject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    companion object {
        const val TAG = "ChatViewModel"
    }

    private val _chatList: MutableLiveData<Event<Resource<List<Message>>>> =
        (repository as DefaultChatRepository).chatList
    val chatList: LiveData<Event<Resource<List<Message>>>> = _chatList

    val playTone: LiveData<Event<Resource<Boolean>>> =
        (repository as DefaultChatRepository).playTone

    val isUserOnline: LiveData<Event<Resource<UserStat>>> =
        (repository as DefaultChatRepository).isUserOnline

    private val _isLastMessagesFirstLoadDone = MutableLiveData<Event<Resource<Boolean>>>()
    val isLastMessagesFirstLoadDone: LiveData<Event<Resource<Boolean>>> =
        _isLastMessagesFirstLoadDone

    fun getChatLoadFirstQuery(
        currentUid: String,
        otherEndUserUid: String,
        otherEndUsername: String
    ) {
        _chatList.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            repository.getChatLoadFirstQuery(currentUid, otherEndUserUid, otherEndUsername)
        }
    }

    fun getChatLoadMore(currentUid: String, otherEndUserUid: String, otherEndUsername: String) {
        _chatList.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            repository.getChatLoadMore(currentUid, otherEndUserUid, otherEndUsername)
        }
    }

    private var uid = ""
    fun setUid(uid: String) {
        this.uid = uid
    }

    val flow = Pager(PagingConfig(Constants.NEW_CHAT_PAGE_SIZE)) {
        FollowingAndFollowersPagingSource(uid, FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope)

    private val _sendMessageStatus = MutableLiveData<Event<Resource<Message>>>()
    val sendMessageStatus: LiveData<Event<Resource<Message>>> = _sendMessageStatus
    fun sendMessage(
        currentUid: String, receiverUid: String,
        message: String, type: String, uri: Uri?,
        replyToMessageId: String?
    ) {
        if (message.isEmpty() && uri == null)
            return
        _sendMessageStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.sendMessage(
                currentUid,
                receiverUid,
                message,
                type,
                uri,
                replyToMessageId
            )
            _sendMessageStatus.postValue(Event(result))
        }
    }

    suspend fun updateMessageStatusAsSeen(message: Message) {
        viewModelScope.launch {
            repository.updateMessageStatusAsSeen(message)
        }
    }

    fun setLastMessageListener(currentUser: User) {
        viewModelScope.launch {
            repository.lastMessageListener(currentUser)
        }
    }

    fun deleteMessage(currentUid: String, otherEndUserUid: String, message: Message) {
        viewModelScope.launch(NonCancellable) {
            repository.deleteChatMessage(currentUid, otherEndUserUid, message)
        }
    }


    fun clearChatList() {
        repository.clearChatList()
    }

    fun checkUserIsOnline(uid: String) {
        viewModelScope.launch {
            repository.checkUserIsOnline(uid)
        }
    }

    fun removeCheckOnlineListener() {
        viewModelScope.launch {
            repository.removeCheckOnlineListener()
        }
    }

    fun removeLastMessageListener() {
        repository.removeLastMessageListener()
    }

    fun getLastMessages() {
        viewModelScope.launch {
            val result = repository.getLastMessages()
            _isLastMessagesFirstLoadDone.postValue(Event(result))
        }
    }

    val lastMessagesFromRoom = repository.getLastMessagesFromRoom()

    fun syncLastMessagesOtherUserData(chatThread: String, uid: String) {
        viewModelScope.launch {
            repository.syncLastMessagesOtherUserData(chatThread, uid)
        }
    }
}