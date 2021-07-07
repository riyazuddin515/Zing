package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import com.riyazuddin.zing.repositories.network.implementation.DefaultChatRepository
import com.riyazuddin.zing.repositories.network.pagingsource.FeedPagingSource
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class HomeViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val chatRepository: ChatRepository
) : BasePostViewModel(repository) {

    private val _loadCurrentUserStatus = MutableLiveData<Event<Resource<User>>>()
    val loadCurrentUserStatus: LiveData<Event<Resource<User>>> = _loadCurrentUserStatus

    val feed = Pager(PagingConfig(POST_PAGE_SIZE)) {
        FeedPagingSource(FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope)

    val haveUnSeenMessages = (chatRepository as DefaultChatRepository).haveUnSeenMessages

    fun checkForUnSeenMessage(uid: String) {
        viewModelScope.launch {
            chatRepository.checkForUnSeenMessage(uid)
        }
    }

    fun removeUnSeenMessageListener() {
        viewModelScope.launch {
            chatRepository.removeUnSeenMessageListener()
        }
    }

    fun onlineOfflineToggle(uid: String) {
        viewModelScope.launch {
            repository.onlineOfflineToggleWithDeviceToken(uid)
        }
    }

    fun getCurrentUser(uid: String) {
        _loadCurrentUserStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.getUserProfile(uid)
            _loadCurrentUserStatus.postValue(Event(result))
        }
    }

    private val _doesUserHaveFollowingRequests = MutableLiveData<Event<Resource<Boolean>>>()
    val doesUserHaveFollowingRequests: LiveData<Event<Resource<Boolean>>> =
        _doesUserHaveFollowingRequests

    fun checkDoesUserHaveFollowerRequests() {
        viewModelScope.launch {
            _doesUserHaveFollowingRequests.postValue(
                Event(repository.checkDoesUserHaveFollowerRequests())
            )
        }
    }

    companion object {
        const val TAG = "HomeViewModel"
    }
}