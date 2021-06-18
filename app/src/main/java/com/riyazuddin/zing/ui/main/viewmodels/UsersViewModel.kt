package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.USERS_LIST_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import com.riyazuddin.zing.repositories.pagingsource.UsersPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class UsersViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _listOfUsersUid = MutableLiveData<Event<Resource<List<String>>>>()
    val listOfUsersUid: LiveData<Event<Resource<List<String>>>> = _listOfUsersUid
    fun getListOfUsersUid(uid: String, source: String) {
        viewModelScope.launch {
            when (source) {
                "Following" ->
                    _listOfUsersUid.postValue(Event(repository.getListOfFollowingUsersUid(uid)))
                "Followers" ->
                    _listOfUsersUid.postValue(Event(repository.getListOfFollowersUsersUid(uid)))
                "LikedBy" ->
                    _listOfUsersUid.postValue(Event(repository.getListOfPostLikes(uid)))
            }
        }
    }

    fun getFlowOfUsers(list: List<String>): Flow<PagingData<User>> =
        Pager(PagingConfig(USERS_LIST_SIZE)) {
            UsersPagingSource(
                list,
                FirebaseFirestore.getInstance().collection(Constants.USERS_COLLECTION)
            )
        }.flow.cachedIn(viewModelScope)
}