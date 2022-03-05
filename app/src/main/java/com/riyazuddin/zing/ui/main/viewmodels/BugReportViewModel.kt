package com.riyazuddin.zing.ui.main.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.riyazuddin.zing.data.entities.BugReport
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _bugReportStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val bugReportStatus: LiveData<Event<Resource<Boolean>>> = _bugReportStatus

    fun submitBug(title: String, description: String) {
        _bugReportStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = mainRepository.reportBug(
                BugReport(title, description, auth.uid!!)
            )
            _bugReportStatus.postValue(Event(result))
        }
    }
}