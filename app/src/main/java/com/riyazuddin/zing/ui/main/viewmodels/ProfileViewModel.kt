package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.data.pagingsource.ProfilePostPagingSource
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : BasePostViewModel(repository) {

    private val _followStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val followStatus: LiveData<Event<Resource<Boolean>>> = _followStatus

    private val _loadProfileMetadata = MutableLiveData<Event<Resource<User>>>()
    val loadProfileMetadata: LiveData<Event<Resource<User>>> = _loadProfileMetadata


    fun getPagingFlowOfPost(uid: String): Flow<PagingData<Post>> {
        val pagingSource = ProfilePostPagingSource(
            FirebaseFirestore.getInstance(),
            uid
        )
        return Pager(PagingConfig(POST_PAGE_SIZE)) {
            pagingSource
        }.flow.cachedIn(viewModelScope)
    }


    fun toggleFollowForUser(uid: String) {
        _followStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.toggleFollowForUser(uid)
            _followStatus.postValue(Event(result))
        }
    }

    fun loadProfile(uid: String) {
        _loadProfileMetadata.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.getUserProfile(uid)
            _loadProfileMetadata.postValue(Event(result))
        }
    }
}