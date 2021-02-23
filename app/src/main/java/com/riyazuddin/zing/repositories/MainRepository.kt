package com.riyazuddin.zing.repositories

import android.net.Uri
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.*
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

    suspend fun updateProfilePic(uid: String, imageUri: Uri): String

    suspend fun searchUsername(query: String): Resource<QuerySnapshot>

    suspend fun verifyAccount(currentPassword: String): Resource<Any>

    suspend fun changePassword(newPassword: String): Resource<Any>

    suspend fun getFollowersList(uid: String): Resource<List<User>>

    suspend fun getFollowing(uid: String): Resource<Following>

    suspend fun getFollowers(uid: String): Resource<Followers>

    suspend fun getPostLikes(postId: String): Resource<PostLikes>

    suspend fun getPostLikedUsers(postId: String): Resource<List<User>>

    suspend fun sendMessage(
        currentUid: String,
        receiverUid: String,
        message: String,
        type: String,
        uri: Uri?,
        senderName: String,
        senderUsername: String,
        senderProfilePicUrl: String,
        receiverName: String,
        receiverUsername: String,
        receiveProfileUrl: String
    ): Resource<Message>

    suspend fun deleteChatMessage(
        currentUid: String,
        receiverUid: String, message: Message
    ): Resource<Message>

    suspend fun getChat(
        currentUid: String,
        otherEndUserUid: String
    ): Resource<FirestoreRecyclerOptions<Message>>

    suspend fun getLastMessageFirestoreRecyclerOptions(uid: String): Resource<FirestoreRecyclerOptions<LastMessage>>
}