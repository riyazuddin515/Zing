package com.riyazuddin.zing.ui.auth.viewmodels

import android.content.Context
import android.util.Patterns
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.MAX_PASSWORD
import com.riyazuddin.zing.other.Constants.MAX_USERNAME
import com.riyazuddin.zing.other.Constants.MIN_PASSWORD
import com.riyazuddin.zing.other.Constants.MIN_USERNAME
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class AuthViewModel @ViewModelInject constructor(
    private val repository: AuthRepository,
    private val applicationContext: Context,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _registerStatus = MutableLiveData<Event<Resource<AuthResult>>>()
    val registerStatus: LiveData<Event<Resource<AuthResult>>> = _registerStatus

    private val _loginStatus = MutableLiveData<Event<Resource<AuthResult>>>()
    val loginStatus: LiveData<Event<Resource<AuthResult>>> = _loginStatus

    private val _passwordResetStatus = MutableLiveData<Event<Resource<String>>>()
    val passwordResetStatus: LiveData<Event<Resource<String>>> = _passwordResetStatus

    private val _isUsernameAvailable = MutableLiveData<Event<Resource<Boolean>>>()
    val isUsernameAvailable: LiveData<Event<Resource<Boolean>>> = _isUsernameAvailable


    fun register(name: String, username: String, email: String, password: String, repeatPassword: String) {
        val error =
            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty())
                applicationContext.getString(R.string.error_fields_can_not_be_empty)
            else if (username.length < MIN_USERNAME)
                applicationContext.getString(R.string.error_username_too_short)
            else if (username.length > MAX_USERNAME)
                applicationContext.getString(R.string.error_username_too_long)
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                applicationContext.getString(R.string.error_not_a_valid_email)
            else if (password.length < MIN_PASSWORD)
                applicationContext.getString(R.string.error_password_too_short)
            else if (password.length > MAX_PASSWORD)
                applicationContext.getString(R.string.error_password_too_long)
            else if (repeatPassword != password)
                applicationContext.getString(R.string.error_password_not_match)
            else null

        error?.let {
            _registerStatus.postValue(Event(Resource.Error(it)))
            return
        }

        _registerStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.register(name, username, email, password)
            _registerStatus.postValue(Event(result))
        }
    }

    fun login(email: String, password: String) {
        val error =
            if (email.isEmpty() || password.isEmpty())
                applicationContext.getString(R.string.error_fields_can_not_be_empty)
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                applicationContext.getString(R.string.error_not_a_valid_email)
            else if (password.length < MIN_PASSWORD)
                applicationContext.getString(R.string.error_password_too_short)
            else if (password.length > MAX_PASSWORD)
                applicationContext.getString(R.string.error_password_too_long)
            else null

        error?.let {
            _loginStatus.postValue(Event(Resource.Error(it)))
            return
        }

        _loginStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.login(email, password)
            _loginStatus.postValue(Event(result))
        }
    }

    fun sendPasswordResetLink(email: String) {
        val error =
            if (email.isEmpty())
                applicationContext.getString(R.string.error_fields_can_not_be_empty)
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                applicationContext.getString(R.string.error_not_a_valid_email)
            else null

        error?.let {
            _passwordResetStatus.postValue(Event(Resource.Error(it)))
            return
        }

        _passwordResetStatus.postValue(Event((Resource.Loading())))
        viewModelScope.launch {
            val result = repository.sendPasswordResetLink(email)
            _passwordResetStatus.postValue(Event(result))
        }
    }

    fun searchUsername(query: String) {
        if (query.isEmpty())
            return

        _isUsernameAvailable.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.searchUsername(query)
            if (result.data!!.isEmpty)
                _isUsernameAvailable.postValue(Event(Resource.Success(true)))
            else _isUsernameAvailable.postValue(Event(Resource.Error("Already taken")))
        }
    }

}