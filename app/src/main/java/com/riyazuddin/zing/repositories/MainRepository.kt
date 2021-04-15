package com.riyazuddin.zing.repositories

import android.net.Uri
import com.algolia.search.model.response.ResponseSearch
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.*
import com.riyazuddin.zing.other.Resource

interface MainRepository {

    /**
     * create a post
     */
    suspend fun createPost(imageUri: Uri, caption: String): Resource<Any>

    /**
     * gets the list of users, with the uid's provided in parameter
     */
    suspend fun getUsers(uids: List<String>): Resource<List<User>>

    /**
     * gets the user document of particular user
     */
    suspend fun getUserProfile(uid: String): Resource<User>

    suspend fun getPostForProfile(uid: String): Resource<List<Post>>

    suspend fun toggleLikeForPost(post: Post): Resource<Boolean>

    suspend fun deletePost(post: Post): Resource<Post>

    /**
     * method used to follow and unFollow the user of uid
     * provide in parameter
     */
    suspend fun toggleFollowForUser(uid: String): Resource<Boolean>

    suspend fun getPostForFollows(): Resource<List<Post>>

    suspend fun createComment(commentText: String, postId: String): Resource<Comment>

    suspend fun getPostComments(postId: String): Resource<List<Comment>>

    suspend fun updateProfile(updateProfile: UpdateProfile, imageUri: Uri? = null): Resource<Any>

    suspend fun updateProfilePic(uid: String, imageUri: Uri): String

    /**
     * method used to check username availability
     */
    suspend fun searchUsername(query: String): Resource<Boolean>

    suspend fun verifyAccount(currentPassword: String): Resource<Any>

    suspend fun changePassword(newPassword: String): Resource<Any>

    suspend fun getFollowersList(uid: String): Resource<Followers>

    suspend fun getFollowingList(uid: String): Resource<Following>

    suspend fun getFollowing(uid: String): Resource<List<User>>

    suspend fun getFollowers(uid: String): Resource<List<User>>

    suspend fun getPostLikes(postId: String): Resource<PostLikes>

    suspend fun getPostLikedUsers(postId: String): Resource<List<User>>

    suspend fun algoliaSearch(searchQuery: String): Resource<ResponseSearch>
}