package com.riyazuddin.zing.ui.auth.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _registerStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val registerStatus: LiveData<Event<Resource<Boolean>>> = _registerStatus
    fun register(email: String, password: String) {
        _registerStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.register(email, password)
            _registerStatus.postValue(Event(result))
        }
    }

    private val _loginStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val loginStatus: LiveData<Event<Resource<Boolean>>> = _loginStatus
    fun login(email: String, password: String) {
        _loginStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.login(email, password)
            _loginStatus.postValue(Event(result))
        }
    }

    private val _passwordResetStatus = MutableLiveData<Event<Resource<String>>>()
    val passwordResetStatus: LiveData<Event<Resource<String>>> = _passwordResetStatus
    fun sendPasswordResetLink(email: String) {
        _passwordResetStatus.postValue(Event((Resource.Loading())))
        viewModelScope.launch {
            val result = repository.sendPasswordResetLink(email)
            _passwordResetStatus.postValue(Event(result))
        }
    }

}