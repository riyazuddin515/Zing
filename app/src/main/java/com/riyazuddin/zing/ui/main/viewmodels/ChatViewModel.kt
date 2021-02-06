package com.riyazuddin.zing.ui.main.viewmodels

import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.TEXT
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _followersList = MutableLiveData<Event<Resource<List<User>>>>()
    val followersList: LiveData<Event<Resource<List<User>>>> = _followersList

    private val _sendMessageStatus = MutableLiveData<Event<Resource<Message>>>()
    val sendMessageStatus: LiveData<Event<Resource<Message>>> = _sendMessageStatus

    private val _lastMessagesOptions =
        MutableLiveData<Event<Resource<FirestoreRecyclerOptions<LastMessage>>>>()
    val lastMessagesOptions: LiveData<Event<Resource<FirestoreRecyclerOptions<LastMessage>>>> =
        _lastMessagesOptions

    private val _chatOptions = MutableLiveData<Event<Resource<FirestoreRecyclerOptions<Message>>>>()
    val chatOptions: LiveData<Event<Resource<FirestoreRecyclerOptions<Message>>>> = _chatOptions


    private val _deleteMessageStatus = MutableLiveData<Event<Resource<Message>>>()
    val deleteMessageStatus: LiveData<Event<Resource<Message>>> = _deleteMessageStatus

    private val _currentUserProfileStatus = MutableLiveData<Event<Resource<User>>>()
    val currentUserProfileStatus: LiveData<Event<Resource<User>>> = _currentUserProfileStatus

    init {
        getCurrentUserProfile()
    }

    private fun getCurrentUserProfile() {
        _currentUserProfileStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.getUserProfile(Firebase.auth.uid!!)
            _currentUserProfileStatus.postValue(Event(result))
        }
    }


    fun getFollowersList(uid: String) {
        if (uid.isEmpty())
            return

        viewModelScope.launch(dispatcher) {
            _followersList.postValue(Event(Resource.Loading()))
            val result = repository.getFollowersList(uid)
            _followersList.postValue(Event(result))
        }

    }

    fun sendMessage(
        currentUid: String, receiverUid: String, message: String, type: String,
        senderName: String,
        senderUsername: String,
        senderProfilePicUrl: String,
        receiverName: String,
        receiverUsername: String,
        receiveProfileUrl: String,
        uri: Uri? = null,
    ) {
        if (message.isEmpty() && type == TEXT)
            return

        Log.e("TAG", "sendMessage: calling repository method")
        viewModelScope.launch(dispatcher) {
            _sendMessageStatus.postValue(Event(Resource.Loading()))
            val result = repository
                .sendMessage(
                    currentUid,
                    receiverUid,
                    message,
                    type,
                    uri,
                    senderName,
                    senderUsername,
                    senderProfilePicUrl,
                    receiverName,
                    receiverUsername,
                    receiveProfileUrl
                )
            Log.e("TAG", "sendMessage: repository method called")
            _sendMessageStatus.postValue(Event(result))
        }
    }


    fun deleteChatMessage(currentUid: String, receiverUid: String, message: Message) {
        viewModelScope.launch(dispatcher) {
            _deleteMessageStatus.postValue(Event(Resource.Loading()))
            val result = repository.deleteChatMessage(currentUid, receiverUid, message)
            _deleteMessageStatus.postValue(Event(result))
        }
    }

    fun getChat(currentUid: String, otherEndUserUid: String) {
        _chatOptions.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.getChat(currentUid, otherEndUserUid)
            _chatOptions.postValue(Event(result))
        }
    }

    fun getLastMessages(uid: String) {
        _lastMessagesOptions.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.getLastMessageFirestoreRecyclerOptions(uid)
            _lastMessagesOptions.postValue(Event(result))
        }
    }
}