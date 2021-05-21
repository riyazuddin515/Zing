package com.riyazuddin.zing.ui.main.viewmodels

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.implementation.DefaultChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class ChatViewModel @ViewModelInject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _sendMessageStatus = MutableLiveData<Event<Resource<Message>>>()
    val sendMessageStatus: LiveData<Event<Resource<Message>>> = _sendMessageStatus

    private val _chatList: MutableLiveData<Event<Resource<List<Message>>>> =
        (repository as DefaultChatRepository).chatList
    val chatList: LiveData<Event<Resource<List<Message>>>> = _chatList

    val playTone: LiveData<Event<Resource<Boolean>>> =
        (repository as DefaultChatRepository).playTone

    private val _recentMessagesList: MutableLiveData<Event<Resource<List<LastMessage>>>> =
        (repository as DefaultChatRepository).lastMessageList
    val recentMessagesList: LiveData<Event<Resource<List<LastMessage>>>> = _recentMessagesList

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

    suspend fun getFollowingAndFollowersUsers(uid: String): Flow<PagingData<User>> {
        return repository.getFollowersAndFollowingForNewChat(uid).data!!.flow.cachedIn(
            viewModelScope
        )
    }

    fun sendMessage(
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
                senderName,
                senderUsername,
                senderProfilePicUrl,
                receiverName,
                receiverUsername,
                receiveProfileUrl
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

    fun getLastMessageFirstQuery() {
        _recentMessagesList.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            repository.getLastMessageFirstQuery()
        }
    }

    fun getLastMessageLoadMore() {
        _recentMessagesList.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            repository.getLastMessageLoadMore()
        }
    }

    fun deleteMessage(currentUid: String, otherEndUserUid: String, message: Message) {
        viewModelScope.launch {
            repository.deleteChatMessage(currentUid, otherEndUserUid, message)
        }
    }


    fun clearChatList() {
        _chatList.postValue(Event(Resource.Success(listOf())))
        repository.clearChatList()
    }

    fun clearRecentMessagesList() {
//        _recentMessagesList.postValue(Event(Resource.Success(listOf())))
        repository.clearRecentMessagesList()
    }

}