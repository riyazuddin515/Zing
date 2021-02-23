package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.pagingsource.FollowingPostPagingSource
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.repositories.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class HomeViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : BasePostViewModel(repository, dispatcher) {

    val pagingFlow = Pager(PagingConfig(POST_PAGE_SIZE)) {
        FollowingPostPagingSource(FirebaseFirestore.getInstance())
    }.flow.cachedIn(viewModelScope)
}