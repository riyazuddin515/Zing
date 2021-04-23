package com.riyazuddin.zing.ui.main.viewmodels

import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.pagingsource.FollowingAndFollowersPagingSource
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import com.riyazuddin.zing.repositories.implementation.DefaultChatRepository
import dagger.hilt.android.scopes.FragmentScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatViewModel @ViewModelInject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _sendMessageStatus = MutableLiveData<Event<Resource<Message>>>()
    val sendMessageStatus: LiveData<Event<Resource<Message>>> = _sendMessageStatus

    private val _chatList:  MutableLiveData<Event<Resource<List<Message>>>> = (repository as DefaultChatRepository).chatList
    val chatList: LiveData<Event<Resource<List<Message>>>> = _chatList

    fun getChatLoadFirstQuery(currentUid: String, otherEndUserUid: String) {
        repository.getChatLoadFirstQuery(currentUid, otherEndUserUid)
    }

    fun getChatLoadMore(currentUid: String, otherEndUserUid: String){
        repository.getChatLoadMore(currentUid, otherEndUserUid)
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
    ){
        if (message.isEmpty() && uri == null)
            return
        _sendMessageStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.sendMessage(currentUid, receiverUid, message, type, uri, senderName, senderUsername, senderProfilePicUrl, receiverName, receiverUsername, receiveProfileUrl)
            _sendMessageStatus.postValue(Event(result))
        }
    }

    fun deleteMessage(currentUid: String, otherEndUserUid: String, message: Message){
        viewModelScope.launch {
            repository.deleteChatMessage(currentUid, otherEndUserUid, message)
        }
    }


    fun clearChatList(){
        _chatList.postValue(Event(Resource.Success(listOf())))
        chatList.value.let {
            Log.i("chatViewModel", "clearChatList: ${it?.getContentHasBeenHandled()?.data?.size}")
            Log.i("chatViewModel", "clearChatList: ${it?.getContentHasBeenHandled()?.data}")
        }
        repository.clearChatList()
    }

}