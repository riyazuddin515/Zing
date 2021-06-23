package com.riyazuddin.zing.repositories.abstraction

import android.net.Uri
import com.algolia.search.model.response.ResponseSearch
import com.riyazuddin.zing.data.entities.*
import com.riyazuddin.zing.other.Resource

interface MainRepository {

    /**
     * @param uid is user's id whom status
     * toggles online, offline, upload device token
     */
    suspend fun onlineOfflineToggleWithDeviceToken(uid: String)

    /**
     * @param uid
     * remove device taken from firestore
     */
    suspend fun removeDeviceToken(uid: String): Resource<Boolean>

    /**
     * @param imageUri
     * @param caption
     * create a post
     */
    suspend fun createPost(imageUri: Uri, caption: String): Resource<Any>


    /**
     * retrieve the post associated with
     * @param postId
     */
    suspend fun getPost(postId: String): Resource<Post>

    /**
     * @param uids
     * gets the list of users
     * @return Resource<List<User>>
     */
    suspend fun getUsers(uids: List<String>): Resource<List<User>>

    /**
     * retrieves the user document associated with uid
     * @param uid
     * @return Resource<User>
     */
    suspend fun getUserProfile(uid: String): Resource<User>

    /**
     * toggle Like for post
     * @param post
     * @return Boolean
     */
    suspend fun toggleLikeForPost(post: Post): Resource<Boolean>

    /**
     * delete post
     * @param post
     * @return Post
     */
    suspend fun deletePost(post: Post): Resource<Post>

    /**
     * Toggle follow and unFollow for uid
     * @param uid for which currentUser is following/unfollowing
     */
    suspend fun toggleFollowForUser(uid: String): Resource<Boolean>

    /**
     * create comment
     * @param commentText --> text of the new comment
     * @param postId --> postId for which new comment belongs
     */
    suspend fun createComment(commentText: String, postId: String): Resource<Comment>

    /**
     * update the user Profile
     * @param updateProfile
     * @param imageUri
     */
    suspend fun updateProfile(updateProfile: UpdateProfile, imageUri: Uri? = null): Resource<Any>

    /**
     * Check username availability
     * @param searchQuery
     * @return Resource<ResponseSearch>
     */
    suspend fun algoliaSearch(searchQuery: String): Resource<ResponseSearch>

    /**
     * Verify current logged in account
     */
    suspend fun verifyAccount(currentPassword: String): Resource<Any>

    /**
     * Change Password
     * @param newPassword
     */
    suspend fun changePassword(newPassword: String): Resource<Any>

    /**
     * @param postId
     * get the PostLikes associate with postId
     * @return PostLikes
     */
    suspend fun getPostLikes(postId: String): Resource<PostLikes>

    /**
     * @param comment
     * Deletes the comment
     * @return Deleted comment
     */
    suspend fun deleteComment(comment: Comment): Resource<Comment>

    suspend fun firebaseUserSearch(query: String): Resource<List<User>>
}