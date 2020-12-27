package com.riyazuddin.zing.repositories

import android.net.Uri
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Resource

interface MainRepository {

    suspend fun createPost(imageUri: Uri, caption: String): Resource<Any>

    suspend fun searchUser(query: String): Resource<List<User>>

    suspend fun getUsers(uids: List<String>): Resource<List<User>>

    suspend fun getUserProfile(uid: String): Resource<User>

    suspend fun getPostForProfile(uid: String): Resource<List<Post>>

    suspend fun toggleLikeForPost(post: Post): Resource<Boolean>

    suspend fun deletePost(post: Post): Resource<Post>

    suspend fun toggleFollowForUser(uid: String): Resource<Boolean>

    suspend fun getPostForFollows(): Resource<List<Post>>
}