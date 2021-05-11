package com.riyazuddin.zing.ui.main.viewmodels

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.pagingsource.FollowingPostPagingSource
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import com.riyazuddin.zing.repositories.implementation.DefaultChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : BasePostViewModel(repository) {

    private val _loadCurrentUserStatus = MutableLiveData<Event<Resource<User>>>()
    val loadCurrentUserStatus: LiveData<Event<Resource<User>>> = _loadCurrentUserStatus

    val pagingFlow = Pager(PagingConfig(POST_PAGE_SIZE)) {
        FollowingPostPagingSource(FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope)

    fun onlineOfflineToggle(uid: String) {
        viewModelScope.launch {
            Log.i(TAG, "onlineOfflineToggle: calling")
            repository.onlineOfflineToggle(uid)
            Log.i(TAG, "onlineOfflineToggle: called")
        }
    }

    fun loadCurrentUser(uid: String) {
        _loadCurrentUserStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.getUserProfile(uid)
            _loadCurrentUserStatus.postValue(Event(result))
        }
    }

    companion object{
        const val TAG = "HomeViewModel"
    }
}