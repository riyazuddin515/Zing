package com.riyazuddin.zing.ui.main.viewmodels

import androidx.lifecycle.*
import androidx.paging.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.USER_PAGE_SIZE
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import com.riyazuddin.zing.repositories.network.pagingsource.UsersPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowerRequestsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val repository: MainRepository
) : ViewModel() {

    private val _followerRequests: MutableLiveData<PagingData<User>> =
        Pager(PagingConfig(USER_PAGE_SIZE)) {
            UsersPagingSource(
                Firebase.auth.uid!!,
                "FollowerRequests",
                firestore
            )
        }.flow.cachedIn(viewModelScope).asLiveData().let {
            it as MutableLiveData<PagingData<User>>
        }
    val followerRequests: LiveData<PagingData<User>> = _followerRequests

    private val _actionStatus = MutableLiveData<Event<Resource<String>>>()
    val actionStatus: LiveData<Event<Resource<String>>> = _actionStatus
    fun acceptOrRejectTheFollowerRequest(uid: String, action: Boolean) {
        _actionStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _actionStatus.postValue(Event(repository.acceptOrRejectTheFollowerRequest(uid, action)))
        }
    }

    fun removeRequestFromLiveData(uid: String) {
        followerRequests.value?.filter {
            it.uid != uid
        }.let {
            _followerRequests.postValue(it)
        }
    }
}