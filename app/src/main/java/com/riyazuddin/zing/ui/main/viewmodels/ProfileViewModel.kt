package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.UserMetadata
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import com.riyazuddin.zing.repositories.network.pagingsource.ProfilePostPagingSource
import kotlinx.coroutines.launch

class ProfileViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : BasePostViewModel(repository) {

    private val _userData = MutableLiveData<Event<Resource<User>>>()
    val userData: LiveData<Event<Resource<User>>> = _userData
    fun loadProfile(uid: String) {
        _userData.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.getUserProfile(uid)
            _userData.postValue(Event(result))
        }
    }

    private lateinit var uid: String
    fun setUid(uid: String) {
        this.uid = uid
    }

    private val _flowOfProfilePosts by lazy {
        Pager(PagingConfig(POST_PAGE_SIZE)) {
            ProfilePostPagingSource(FirebaseFirestore.getInstance(), uid)
        }.flow.cachedIn(viewModelScope).asLiveData().let {
            it as MutableLiveData<PagingData<Post>>
        }
    }
    val flowOfProfilePosts: LiveData<PagingData<Post>> = _flowOfProfilePosts

    fun otherProfileFeed(uid: String) = Pager(PagingConfig(POST_PAGE_SIZE)) {
        ProfilePostPagingSource(FirebaseFirestore.getInstance(), uid)
    }.flow.cachedIn(viewModelScope)

    private val _followStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val followStatus: LiveData<Event<Resource<Boolean>>> = _followStatus
    fun toggleFollowForUser(uid: String) {
        _followStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.toggleFollowForUser(uid)
            _followStatus.postValue(Event(result))
        }
    }

    fun removeFromLiveData(post: Post) {
        flowOfProfilePosts.value?.filter {
            it.postId != post.postId
        }.let {
            _flowOfProfilePosts.postValue(it)
        }
    }

    private val _userMetadata = MutableLiveData<Event<Resource<UserMetadata>>>()
    val userMetadata: LiveData<Event<Resource<UserMetadata>>> = _userMetadata
    fun getUserMetaData(uid: String) {
        _userMetadata.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _userMetadata.postValue(Event(repository.getUserMetaData(uid)))
        }
    }

    private val _toggleSendFollowingRequest = MutableLiveData<Event<Resource<Boolean>>>()
    val toggleSendFollowingRequest: LiveData<Event<Resource<Boolean>>> = _toggleSendFollowingRequest
    fun toggleSendFollowerRequest(uid: String) {
        _toggleSendFollowingRequest.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _toggleSendFollowingRequest.postValue(
                Event(repository.toggleSendFollowerRequest(uid))
            )
        }
    }

    private val _previousFollowingRequests = MutableLiveData<Event<Resource<Boolean>>>()
    val previousFollowingRequests: LiveData<Event<Resource<Boolean>>> = _previousFollowingRequests
    fun checkForPreviousFollowerRequests(uid: String) {
        _previousFollowingRequests.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _previousFollowingRequests.postValue(
                Event(repository.checkForPreviousFollowerRequests(uid))
            )
        }
    }
}