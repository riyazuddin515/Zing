package com.riyazuddin.zing.ui.main.viewmodels

import android.content.Context
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import com.riyazuddin.zing.repositories.pagingsource.FeedPagingSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : BasePostViewModel(repository) {

    private val _loadCurrentUserStatus = MutableLiveData<Event<Resource<User>>>()
    val loadCurrentUserStatus: LiveData<Event<Resource<User>>> = _loadCurrentUserStatus

    private val _removeDeviceTokeStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val removeDeviceTokeStatus : LiveData<Event<Resource<Boolean>>> = _removeDeviceTokeStatus

    private val _feedPagingFlow = Pager(PagingConfig(POST_PAGE_SIZE)) {
        FeedPagingSource(FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope).asLiveData().let {
        it as MutableLiveData<PagingData<Post>>
    }
    val feedPagingFlow: LiveData<PagingData<Post>> = _feedPagingFlow

    fun onlineOfflineToggle(uid: String) {
        viewModelScope.launch {
            Log.i(TAG, "onlineOfflineToggle: calling")
            repository.onlineOfflineToggleWithDeviceToken(uid)
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

    fun removeDeviceToken(uid: String) {
        _removeDeviceTokeStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.removeDeviceToken(uid)
            _removeDeviceTokeStatus.postValue(Event(result))
        }
    }

    fun removePostFromLiveData(post: Post) {
        feedPagingFlow.value?.filter {
            it.postId != post.postId
        }.let {
            _feedPagingFlow.postValue(it)
        }
    }

    companion object {
        const val TAG = "HomeViewModel"
    }
}