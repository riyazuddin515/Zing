package com.riyazuddin.zing.other

import androidx.lifecycle.Observer

class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentHasBeenHandled(): T? {
        return if (!hasBeenHandled) {
            hasBeenHandled = true
            content
        } else null
    }

    fun peekContent() = content
}

class EventObserver<T>(
    private inline val onError: ((String) -> Unit)? = null,
    private inline val onLoading: (() -> Unit)? = null,
    private inline val onSuccess: ((T) -> Unit)
) : Observer<Event<Resource<T>>> {
    override fun onChanged(t: Event<Resource<T>>?) {
        when (val content = t?.peekContent()) {
            is Resource.Success -> {
                t.getContentHasBeenHandled()?.let {
                    content.data?.let(onSuccess)
                }
            }
            is Resource.Error -> {
                t.getContentHasBeenHandled()?.let {
                    onError?.let { error ->
                        error(it.message!!)
                    }
                }
            }
            is Resource.Loading -> {
                onLoading?.let { loading ->
                    loading()
                }
            }
        }
    }
}