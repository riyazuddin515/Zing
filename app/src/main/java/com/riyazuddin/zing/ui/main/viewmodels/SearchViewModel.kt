package com.riyazuddin.zing.ui.main.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algolia.search.model.response.ResponseSearch
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _algoliaSearchResult = MutableLiveData<Event<Resource<ResponseSearch>>>()
    val algoliaSearchResult: LiveData<Event<Resource<ResponseSearch>>> = _algoliaSearchResult

    fun search(query: String) {
        if (query.isEmpty())
            return
        _algoliaSearchResult.postValue(Event(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.algoliaSearch(query)
            _algoliaSearchResult.postValue(Event(result))
        }
    }

//    private val _firebaseUserSearchResult = MutableLiveData<Event<Resource<List<User>>>>()
//    val firebaseUserSearchResult: LiveData<Event<Resource<List<User>>>> = _firebaseUserSearchResult
//    fun firebaseUserSearch(query: String){
//        if (query.isEmpty())
//            return
//        _firebaseUserSearchResult.postValue(Event(Resource.Loading()))
//        viewModelScope.launch {
//            val result = repository.firebaseUserSearch(query)
//            _firebaseUserSearchResult.postValue(Event(result))
//        }
//    }
}