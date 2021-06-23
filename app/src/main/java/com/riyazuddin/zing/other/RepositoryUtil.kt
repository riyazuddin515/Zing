package com.riyazuddin.zing.other

import android.util.Log

const val TAG = "safeCall"

inline fun <T> safeCall(action: () -> Resource<T>): Resource<T> {
    return try {
        action()
    } catch (e: Exception) {
        Log.e(TAG, "safeCall: ", e)
        Resource.Error(e.localizedMessage ?: "Unknown Error.")
    }
}