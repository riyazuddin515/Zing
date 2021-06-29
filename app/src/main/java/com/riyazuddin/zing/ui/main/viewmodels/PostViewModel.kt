package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import kotlinx.coroutines.launch

class PostViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private var _post = MutableLiveData<Event<Resource<Post>>>()
    val post: LiveData<Event<Resource<Post>>> = _post

    fun getPost(postId: String) {
        _post.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _post.postValue(Event(repository.getPost(postId)))
        }
    }
}