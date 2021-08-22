package com.riyazuddin.zing.ui.main.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.GetStreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.models.User
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GetStreamViewModel @Inject constructor(
    private val repository: GetStreamRepository
) : ViewModel() {

    private val _connectUserStatus = MutableLiveData<Event<Resource<User>>>()
    val connectUserStatus: LiveData<Event<Resource<User>>> = _connectUserStatus
    fun connectUser(user: User) {
        viewModelScope.launch {
            val result = repository.connectUser(user)
            _connectUserStatus.postValue(Event(result))
        }
    }

    private val _createChannelStatus = MutableLiveData<Event<Resource<String>>>()
    val createChannelStatus: LiveData<Event<Resource<String>>> = _createChannelStatus
    fun createChatChannel(currentUid: String, otherEndUserUid: String) {
        _createChannelStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.createChatChannel(currentUid, otherEndUserUid)
            _createChannelStatus.postValue(Event(result))
        }
    }

    private val _updateNotificationTokenStatus = MutableLiveData<Event<Resource<Unit>>>()
    val updateNotificationTokenStatus: LiveData<Event<Resource<Unit>>> =
        _updateNotificationTokenStatus

    fun setFCMTokenInStream() {
        _updateNotificationTokenStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _updateNotificationTokenStatus.postValue(
                Event(repository.setFCMTokenInStream())
            )
        }
    }
}