package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
): BasePostViewModel(repository, dispatcher) {

    private val _posts = MutableLiveData<Event<Resource<List<Post>>>>()
    override val posts: LiveData<Event<Resource<List<Post>>>>
        get() = _posts

    init {
        getPosts()
    }

    override fun getPosts(uid: String) {
        _posts.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher){
            val result = repository.getPostForFollows()
            _posts.postValue(Event(result))
        }
    }
}