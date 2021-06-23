package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import com.riyazuddin.zing.repositories.implementation.DefaultChatRepository
import com.riyazuddin.zing.repositories.pagingsource.FeedPagingSource
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class HomeViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val chatRepository: ChatRepository
) : BasePostViewModel(repository) {

    private val _loadCurrentUserStatus = MutableLiveData<Event<Resource<User>>>()
    val loadCurrentUserStatus: LiveData<Event<Resource<User>>> = _loadCurrentUserStatus

    private val _removeDeviceTokeStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val removeDeviceTokeStatus: LiveData<Event<Resource<Boolean>>> = _removeDeviceTokeStatus

    private val _feed: MutableLiveData<PagingData<Post>> =
        Pager(PagingConfig(POST_PAGE_SIZE)) {
            FeedPagingSource(FirebaseFirestore.getInstance())
        }.flow.cachedIn(viewModelScope).asLiveData().let {
            it as MutableLiveData<PagingData<Post>>
        }
    val feed: LiveData<PagingData<Post>> = _feed

    val haveUnSeenMessages = (chatRepository as DefaultChatRepository).haveUnSeenMessages

    fun checkForUnSeenMessage(uid: String) {
        viewModelScope.launch {
            chatRepository.checkForUnSeenMessage(uid)
        }
    }
    fun removeUnSeenMessageListener() {
        chatRepository.removeUnSeenMessageListener()
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

    fun removeDeviceToken(uid: String) {
        _removeDeviceTokeStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.removeDeviceToken(uid)
            _removeDeviceTokeStatus.postValue(Event(result))
        }
    }

    fun removePostFromLiveData(post: Post) {
        feed.value?.filter {
            it.postId != post.postId
        }.let {
            _feed.postValue(it)
        }
    }

    companion object {
        const val TAG = "HomeViewModel"
    }
}