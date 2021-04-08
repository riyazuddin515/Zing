package com.riyazuddin.zing.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algolia.search.model.response.ResponseSearch
import com.riyazuddin.zing.other.Event
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
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
}