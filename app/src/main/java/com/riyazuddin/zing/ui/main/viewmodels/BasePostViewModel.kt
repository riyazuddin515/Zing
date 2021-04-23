package com.riyazuddin.zing.ui.main.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BasePostViewModel(
    private val repository: MainRepository
) : ViewModel() {

    private val _likePostStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val likePostStatus: LiveData<Event<Resource<Boolean>>> = _likePostStatus

    private val _deletePostStatus = MutableLiveData<Event<Resource<Post>>>()
    val deletePostStatus: LiveData<Event<Resource<Post>>> = _deletePostStatus

    private val _postLikedUsersStatus = MutableLiveData<Event<Resource<List<User>>>>()
    val postLikedUsersStatus: LiveData<Event<Resource<List<User>>>> = _postLikedUsersStatus

    fun getPostLikedUsers(postId: String) {
        if (postId.isEmpty())
            return
        _postLikedUsersStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch{
            val result = repository.getPostLikedUsers(postId)
            _postLikedUsersStatus.postValue(Event(result))
        }
    }

    fun toggleLikeForPost(post: Post) {
        _likePostStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.toggleLikeForPost(post)
            _likePostStatus.postValue(Event(result))
        }
    }

    fun deletePost(post: Post) {
        _deletePostStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.deletePost(post)
            _deletePostStatus.postValue(Event(result))
        }
    }


}