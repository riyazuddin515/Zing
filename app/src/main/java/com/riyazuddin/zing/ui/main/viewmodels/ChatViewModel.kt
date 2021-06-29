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
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.entities.UserStat
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.network.implementation.DefaultChatRepository
import com.riyazuddin.zing.repositories.network.pagingsource.FollowingAndFollowersPagingSource
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class ChatViewModel @ViewModelInject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    companion object{
        const val TAG = "ChatViewModel"
    }
    private val _sendMessageStatus = MutableLiveData<Event<Resource<Message>>>()
    val sendMessageStatus: LiveData<Event<Resource<Message>>> = _sendMessageStatus

    private val _chatList: MutableLiveData<Event<Resource<List<Message>>>> =
        (repository as DefaultChatRepository).chatList
    val chatList: LiveData<Event<Resource<List<Message>>>> = _chatList

    val playTone: LiveData<Event<Resource<Boolean>>> =
        (repository as DefaultChatRepository).playTone

    private val _recentMessagesList: MutableLiveData<List<LastMessage>> =
        (repository as DefaultChatRepository).lastMessageList
    val recentMessagesList: LiveData<List<LastMessage>> = _recentMessagesList

    val isUserOnline: LiveData<Event<Resource<UserStat>>> = (repository as DefaultChatRepository).isUserOnline

    private val _isLastMessagesFirstLoadDone = MutableLiveData<Event<Resource<Boolean>>>()
    val isLastMessagesFirstLoadDone: LiveData<Event<Resource<Boolean>>> = _isLastMessagesFirstLoadDone

    fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String) {
        _chatList.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            repository.getChatLoadFirstQuery(currentUid, otherEndUserUid)
        }
    }

    fun getChatLoadMore(currentUid: String, otherEndUserUid: String) {
        _chatList.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            repository.getChatLoadMore(currentUid, otherEndUserUid)
        }
    }

    private var uid = ""
    fun setUid(uid: String){
        this.uid = uid
    }
    val flow = Pager(PagingConfig(Constants.NEW_CHAT_PAGE_SIZE)) {
        FollowingAndFollowersPagingSource(uid, FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope)

    fun sendMessage(
        currentUid: String,
        receiverUid: String,
        message: String,
        type: String,
        uri: Uri?,
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
                uri
            )
            _sendMessageStatus.postValue(Event(result))
        }
    }

    fun updateChatListOnMessageSent(message: Message) {
        repository.updateChatListOnMessageSent(message)
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
    fun getLastMessageFirstQuery(currentUser: User) {
        viewModelScope.launch {
            val result = repository.getLastMessageFirstQuery(currentUser)
            _isLastMessagesFirstLoadDone.postValue(Event(result))
        }
    }

    fun getLastMessageLoadMore(currentUser: User) {
        viewModelScope.launch {
            repository.getLastMessageLoadMore(currentUser)
        }
    }

    private val _messageDeleteStatus = MutableLiveData<Event<Resource<Message>>>()
    val messageDeleteStatus: LiveData<Event<Resource<Message>>> = _messageDeleteStatus
    fun deleteMessage(currentUid: String, otherEndUserUid: String, message: Message) {
        viewModelScope.launch {
            _messageDeleteStatus.postValue(
                Event(repository.deleteChatMessage(currentUid, otherEndUserUid, message))
            )
        }
    }


    fun clearChatList() {
        repository.clearChatList()
    }

    fun clearRecentMessagesList() {
        repository.clearRecentMessagesList()
    }

    fun checkUserIsOnline(uid: String) {
        viewModelScope.launch {
            repository.checkUserIsOnline(uid)
        }
    }
    fun removeCheckOnlineListener(){
        repository.removeCheckOnlineListener()
    }
}