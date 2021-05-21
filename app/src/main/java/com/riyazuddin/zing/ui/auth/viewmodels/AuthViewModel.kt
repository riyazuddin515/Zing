package com.riyazuddin.zing.ui.auth.viewmodels

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.AuthRepository
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import kotlinx.coroutines.launch

class AuthViewModel @ViewModelInject constructor(
    private val repository: AuthRepository,
    private val mainRepository: MainRepository,
) : ViewModel() {

    private val _registerStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val registerStatus: LiveData<Event<Resource<Boolean>>> = _registerStatus

    private val _loginStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val loginStatus: LiveData<Event<Resource<Boolean>>> = _loginStatus

    private val _passwordResetStatus = MutableLiveData<Event<Resource<String>>>()
    val passwordResetStatus: LiveData<Event<Resource<String>>> = _passwordResetStatus

//    private val _isUsernameAvailable = MutableLiveData<Event<Resource<Boolean>>>()
//    val isUsernameAvailable: LiveData<Event<Resource<Boolean>>> = _isUsernameAvailable

    private val _isUsernameAvailable = MutableLiveData<Event<Resource<Boolean>>>()
    val isUsernameAvailable: LiveData<Event<Resource<Boolean>>> = _isUsernameAvailable


    fun register(
        name: String,
        username: String,
        email: String,
        password: String
    ) {
        _registerStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.register(name, username, email, password)
            _registerStatus.postValue(Event(result))
        }
    }

    fun login(email: String, password: String) {
        _loginStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.login(email, password)
            _loginStatus.postValue(Event(result))
        }
    }

    fun sendPasswordResetLink(email: String) {
        _passwordResetStatus.postValue(Event((Resource.Loading())))
        viewModelScope.launch {
            val result = repository.sendPasswordResetLink(email)
            _passwordResetStatus.postValue(Event(result))
        }
    }

    fun searchUsername(query: String) {
        _isUsernameAvailable.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.searchUsername(query)
            if (result.data!!)
                _isUsernameAvailable.postValue(Event(Resource.Success(true)))
            else
                _isUsernameAvailable.postValue(Event(Resource.Error("Already taken")))
        }
    }

    fun checkUserNameAvailability(query: String) {
        var exists = false
        _isUsernameAvailable.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = mainRepository.algoliaSearch(query)
            result.data?.hits.let { hits ->
                hits?.forEach { hit ->
                    val username = hit.json.getValue("username").toString().replace("\"", "")
                    Log.i("AuthViewModel", "checkUserNameAvailability: $username")
                    if (username.equals(query, true)) {
                        exists = true
                        Log.i("AuthViewModel", "checkUserNameAvailability: Matched")
                        return@let
                    }
                }
            }
            if (exists) {
                _isUsernameAvailable.postValue(Event(Resource.Error("Already taken")))
            } else {
                _isUsernameAvailable.postValue(Event(Resource.Success(true)))
            }
        }
    }

}