package com.riyazuddin.zing.ui.main.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyazuddin.zing.data.entities.UpdateProfile
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _currentImageUri = MutableLiveData<Uri>()
    val currentImageUri: LiveData<Uri> = _currentImageUri

    fun setImageUri(imageUri: Uri) {
        _currentImageUri.postValue(imageUri)
    }

    private val _croppedImageUri = MutableLiveData<Uri>()
    val croppedImageUri: LiveData<Uri> = _croppedImageUri

    fun setCroppedImageUri(imageUri: Uri) {
        _croppedImageUri.postValue(imageUri)
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

    private val _changePasswordStatus = MutableLiveData<Event<Resource<String>>>()
    val changePasswordStatus: LiveData<Event<Resource<String>>> = _changePasswordStatus
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

    private val _isUsernameAvailable = MutableLiveData<Event<Resource<Boolean>>>()
    val isUsernameAvailable: LiveData<Event<Resource<Boolean>>> = _isUsernameAvailable
    fun checkUserNameAvailability(query: String) {
        var exists = false
        _isUsernameAvailable.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.algoliaSearch(query)
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