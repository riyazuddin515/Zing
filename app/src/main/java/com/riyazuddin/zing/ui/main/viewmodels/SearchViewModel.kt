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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _searchUserStatus = MutableLiveData<Event<Resource<List<User>>>>()
    val searchUserStatus: LiveData<Event<Resource<List<User>>>> = _searchUserStatus

    fun searchUser(query: String) {
        if (query.isEmpty())
            return

        _searchUserStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.searchUser(query)
            _searchUserStatus.postValue(Event(result))
        }
    }
}