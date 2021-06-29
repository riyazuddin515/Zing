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
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import kotlinx.coroutines.launch

class SettingsViewModel @ViewModelInject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri> = _imageUri
    fun setImage(uri: Uri) {
        _imageUri.postValue(uri)
    }

    private val _userProfileStatus = MutableLiveData<Event<Resource<User>>>()
    val userProfileStatus: LiveData<Event<Resource<User>>> = _userProfileStatus
    fun getUserProfile(uid: String) = viewModelScope.launch {
        _userProfileStatus.postValue(Event(Resource.Loading()))
        val result = repository.getUserProfile(uid)
        _userProfileStatus.postValue(Event(result))
    }

    private val _updateProfileStatus = MutableLiveData<Event<Resource<Any>>>()
    val updateProfileStatus: LiveData<Event<Resource<Any>>> = _updateProfileStatus
    fun updateProfile(updateProfile: UpdateProfile, imageUri: Uri?) =
        viewModelScope.launch {
            _updateProfileStatus.postValue(Event(Resource.Loading()))
            val result = repository.updateProfile(updateProfile, imageUri)
            _updateProfileStatus.postValue(Event(result))
        }


    private val _currentPasswordVerificationStatus = MutableLiveData<Event<Resource<Any>>>()
    val currentPasswordVerificationStatus: LiveData<Event<Resource<Any>>> =
        _currentPasswordVerificationStatus

    fun verifyAccount(currentPassword: String) {
        if (currentPassword.isEmpty())
            return
        viewModelScope.launch {
            _currentPasswordVerificationStatus.postValue(Event(Resource.Loading()))
            val result = repository.verifyAccount(currentPassword)
            _currentPasswordVerificationStatus.postValue(Event(result))
        }
    }

    private val _changePasswordStatus = MutableLiveData<Event<Resource<Any>>>()
    val changePasswordStatus: LiveData<Event<Resource<Any>>> = _changePasswordStatus
    fun changePassword(newPassword: String) {
        _changePasswordStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.changePassword(newPassword)
            _changePasswordStatus.postValue(Event(result))
        }
    }

    private val _removeDeviceTokeStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val removeDeviceTokeStatus: LiveData<Event<Resource<Boolean>>> = _removeDeviceTokeStatus
    fun removeDeviceToken(uid: String) {
        _removeDeviceTokeStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.removeDeviceToken(uid)
            _removeDeviceTokeStatus.postValue(Event(result))
        }
    }

    private val _togglePrivacyStatus = MutableLiveData<Event<Resource<String>>>()
    val togglePrivacyStatus: LiveData<Event<Resource<String>>> = _togglePrivacyStatus
    fun toggleAccountPrivacy(uid: String, privacy: String) {
        _togglePrivacyStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _togglePrivacyStatus.postValue(
                Event(repository.toggleAccountPrivacy(uid, privacy))
            )
        }
    }
}