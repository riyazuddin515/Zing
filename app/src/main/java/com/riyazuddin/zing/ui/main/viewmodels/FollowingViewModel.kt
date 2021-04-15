package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.MainRepository
import kotlinx.coroutines.launch

class FollowingViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _followingListUsers = MutableLiveData<Event<Resource<List<User>>>>()
    val followingListUsers: LiveData<Event<Resource<List<User>>>> = _followingListUsers

    fun getFollowing(uid: String) {
        if (uid.isEmpty())
            return
        _followingListUsers.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.getFollowers(uid)
            _followingListUsers.postValue(Event(result))
        }
    }
}