package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.Comment
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.COMMENTS_COLLECTION
import com.riyazuddin.zing.other.Constants.COMMENT_PAGE_SIZE
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import com.riyazuddin.zing.repositories.network.pagingsource.PostCommentsPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {


    private val _createCommentStatus = MutableLiveData<Event<Resource<Comment>>>()
    val createCommentStatus: LiveData<Event<Resource<Comment>>> = _createCommentStatus

    private val _deleteCommentStatus = MutableLiveData<Event<Resource<Comment>>>()
    val deleteCommentStatus: LiveData<Event<Resource<Comment>>> = _deleteCommentStatus

    private val _userProfileStatus = MutableLiveData<Event<Resource<User>>>()
    val userProfileStatus: LiveData<Event<Resource<User>>> = _userProfileStatus

    private lateinit var postId: String

    private var _postComments: MutableLiveData<PagingData<Comment>> =
        Pager(PagingConfig(COMMENT_PAGE_SIZE)) {
            PostCommentsPagingSource(
                Firebase.firestore.collection(COMMENTS_COLLECTION).document(postId).collection(
                    COMMENTS_COLLECTION
                ),
                Firebase.firestore.collection(USERS_COLLECTION)
            )
        }.flow.cachedIn(viewModelScope).asLiveData().let {
            it as MutableLiveData<PagingData<Comment>>
        }
    val postComments: LiveData<PagingData<Comment>> = _postComments

    fun createComment(commentText: String, postId: String) {
        if (commentText.isEmpty())
            return
        _createCommentStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            _createCommentStatus.postValue(Event(repository.createComment(commentText, postId)))
        }
    }

    fun deleteComment(comment: Comment) {
        _deleteCommentStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            _deleteCommentStatus.postValue(Event(repository.deleteComment(comment)))
        }
    }

    /**
     * Just Store the postId in global postId variable
     * From which pager load comments
     */
    fun getComments(postId: String) {
        this.postId = postId
    }

    fun insertCommentInLiveData(comment: Comment) {
        _postComments.postValue(postComments.value?.insertHeaderItem(comment))
    }

    fun deleteCommentInLiveData(comment: Comment) {
        postComments.value?.filter {
            it.commentId != comment.commentId
        }.let {
            _postComments.postValue(it)
        }
    }

    fun getUserProfile() {
        _userProfileStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            _userProfileStatus.postValue(Event(repository.getUserProfile(Firebase.auth.uid!!)))
        }
    }

}