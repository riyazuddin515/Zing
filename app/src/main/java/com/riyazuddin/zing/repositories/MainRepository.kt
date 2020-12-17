package com.riyazuddin.zing.repositories

import android.net.Uri
import com.riyazuddin.zing.other.Resource

interface MainRepository {

    suspend fun createPost(imageUri: Uri, caption: String?): Resource<Any>
}