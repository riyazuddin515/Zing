package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : BasePostViewModel(repository, dispatcher) {

    private val _followStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val followStatus: LiveData<Event<Resource<Boolean>>> = _followStatus

    private val _loadProfileMetadata = MutableLiveData<Event<Resource<User>>>()
    val loadProfileMetadata: LiveData<Event<Resource<User>>> = _loadProfileMetadata

    private val _posts = MutableLiveData<Event<Resource<List<Post>>>>()
    override val posts: LiveData<Event<Resource<List<Post>>>>
        get() = _posts

    override fun getPosts(uid: String) {
        _posts.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.getPostForProfile(uid)
            _posts.postValue(Event(result))
        }
    }

    fun toggleFollowForUser(uid: String) {
        _followStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.toggleFollowForUser(uid)
            _followStatus.postValue(Event(result))
        }
    }

    fun loadProfile(uid: String){
        _loadProfileMetadata.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher){
            val result = repository.getUserProfile(uid)
            _loadProfileMetadata.postValue(Event(result))
        }
        getPosts(uid)
    }
}