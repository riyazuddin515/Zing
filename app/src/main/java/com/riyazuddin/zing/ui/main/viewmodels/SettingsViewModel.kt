package com.riyazuddin.zing.ui.main.viewmodels

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.data.entities.UpdateProfile
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel(){

    private val _userProfileStatus = MutableLiveData<Event<Resource<User>>>()
    val userProfileStatus: LiveData<Event<Resource<User>>> = _userProfileStatus

    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri> = _imageUri

    private val _isUsernameAvailable = MutableLiveData<Event<Resource<Boolean>>>()
    val isUsernameAvailable: LiveData<Event<Resource<Boolean>>> = _isUsernameAvailable

    private val _updateProfileStatus = MutableLiveData<Event<Resource<Any>>>()
    val updateProfileStatus: LiveData<Event<Resource<Any>>> = _updateProfileStatus

    fun getUserProfile(uid: String) = viewModelScope.launch{
        _userProfileStatus.postValue(Event(Resource.Loading()))
        val result = repository.getUserProfile(uid)
        _userProfileStatus.postValue(Event(result))
    }

    fun setImage(uri: Uri){
        _imageUri.postValue(uri)
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

    fun updateProfile(updateProfile: UpdateProfile, imageUri: Uri?) = viewModelScope.launch{
        _updateProfileStatus.postValue(Event(Resource.Loading()))
        val result = repository.updateProfile(updateProfile, imageUri)
        _updateProfileStatus.postValue(Event(result))
    }



}