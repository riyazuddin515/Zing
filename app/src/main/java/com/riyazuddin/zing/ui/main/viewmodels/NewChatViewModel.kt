package com.riyazuddin.zing.ui.main.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.other.Constants.NEW_CHAT_PAGE_SIZE
import com.riyazuddin.zing.repositories.network.pagingsource.NewChatPagingSource

class NewChatViewModel: ViewModel() {

    fun flow(uid: String) = Pager(PagingConfig(NEW_CHAT_PAGE_SIZE)) {
        NewChatPagingSource(uid, FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope)
}