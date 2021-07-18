package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.other.Constants.USER_PAGE_SIZE
import com.riyazuddin.zing.repositories.network.pagingsource.FollowingAndFollowersPagingSource
import com.riyazuddin.zing.repositories.network.pagingsource.UsersPagingSource

class UsersViewModel @ViewModelInject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    fun flowOfUsersForLikesAndFollowerRequest(uid: String, title: String) = Pager(PagingConfig(USER_PAGE_SIZE)) {
        UsersPagingSource(uid, title, firestore)
    }.flow.cachedIn(viewModelScope)

    fun flowOfUsersForFollowingAndFollowers(uid: String, title: String) = Pager(
        PagingConfig(
            USER_PAGE_SIZE)){
        FollowingAndFollowersPagingSource(uid, title, firestore)
    }.flow.cachedIn(viewModelScope)

}