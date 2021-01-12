package com.riyazuddin.zing.repositories

import android.net.Uri
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Comment
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.UpdateProfile
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

    suspend fun createComment(commentText: String, postId: String): Resource<Comment>

    suspend fun getPostComments(postId: String): Resource<List<Comment>>

    suspend fun updateProfile(updateProfile: UpdateProfile, imageUri: Uri? = null): Resource<Any>

    suspend fun updateProfilePic(uid: String, imageUri: Uri) : String

    suspend fun searchUsername(query: String) : Resource<QuerySnapshot>
}