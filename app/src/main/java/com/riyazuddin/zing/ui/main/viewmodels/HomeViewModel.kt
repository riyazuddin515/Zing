package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import com.riyazuddin.zing.repositories.network.pagingsource.FeedPagingSource
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class HomeViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : BasePostViewModel(repository) {

    private val _loadCurrentUserStatus = MutableLiveData<Event<Resource<User>>>()
    val loadCurrentUserStatus: LiveData<Event<Resource<User>>> = _loadCurrentUserStatus

    private val _feedPagingFlow = Pager(PagingConfig(POST_PAGE_SIZE)) {
        FeedPagingSource(FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope).asLiveData().let {
        it as MutableLiveData<PagingData<Post>>
    }
    val feedPagingFlow: LiveData<PagingData<Post>> = _feedPagingFlow

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