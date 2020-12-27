package com.riyazuddin.zing.repositories

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POSTS_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class DefaultMainRepository : MainRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection(USERS_COLLECTION)
    private val postsCollection = firestore.collection(POSTS_COLLECTION)
    private val storage = FirebaseStorage.getInstance()

    override suspend fun createPost(imageUri: Uri, caption: String) = withContext(Dispatchers.IO) {
        safeCall {
            val uid = auth.uid!!
            val postID = UUID.randomUUID().toString()
            val postDownloadUrl = storage.reference.child("posts/$uid/$postID").putFile(imageUri)
                .await().metadata?.reference?.downloadUrl?.await().toString()
            val post = Post(postID, uid, System.currentTimeMillis(), postDownloadUrl, caption)
            postsCollection.document(postID).set(post).await()
            Resource.Success(Any())
        }
    }

    override suspend fun searchUser(query: String) = withContext(Dispatchers.IO) {
        safeCall {
            val usersList = usersCollection.whereGreaterThanOrEqualTo(
                "username",
                query.toUpperCase(Locale.ROOT)
            )
                .get().await().toObjects(User::class.java)
            Resource.Success(usersList)
        }
    }

    override suspend fun getUserProfile(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            val user = usersCollection.document(uid).get().await().toObject(User::class.java)
                ?: throw IllegalStateException()
            val currentUid = auth.uid!!
            val currentUser =
                usersCollection.document(currentUid).get().await().toObject(User::class.java)
                    ?: throw IllegalStateException()

            user.isFollowing = uid in currentUser.follows
            Resource.Success(user)
        }
    }

    override suspend fun getUsers(uids: List<String>) = withContext(Dispatchers.IO) {
        safeCall {
            val usersList =
                usersCollection.whereIn("uid", uids).orderBy("username").get().await()
                    .toObjects(User::class.java)
            Resource.Success(usersList)
        }
    }

    override suspend fun getPostForProfile(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            val posts = postsCollection
                .whereEqualTo("authorUid", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
                .onEach { post ->
                    val user = getUserProfile(uid).data!!
                    post.username = user.username
                    post.userProfilePic = user.profilePicUrl
                    post.isLiked = uid in post.likedBy
                }
            Resource.Success(posts)
        }
    }

    override suspend fun toggleLikeForPost(post: Post) = withContext(Dispatchers.IO) {
        safeCall {
            var isLiked = false
            firestore.runTransaction { transition ->
                val uid = auth.uid!!
                val currentLikes = transition.get(postsCollection.document(post.postId))
                    .toObject(Post::class.java)?.likedBy ?: listOf()
                transition.update(
                    postsCollection.document(post.postId),
                    "likedBy",
                    if (uid in currentLikes) currentLikes - uid
                    else {
                        isLiked = true
                        currentLikes + uid
                    }
                )
            }.await()
            Resource.Success(isLiked)
        }
    }

    override suspend fun deletePost(post: Post) = withContext(Dispatchers.IO) {
        safeCall {
            postsCollection.document(post.postId).delete().await()
            storage.getReferenceFromUrl(post.imageUrl).delete().await()
            Resource.Success(post)
        }
    }

    override suspend fun toggleFollowForUser(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            var isFollowing = false
            firestore.runTransaction { transition ->
                val currentUid = auth.uid!!
                val currentUser =
                    transition.get(usersCollection.document(currentUid))
                        .toObject(User::class.java)!!
                isFollowing = uid in currentUser.follows
                val newFollows =
                    if (isFollowing)
                        currentUser.follows - uid
                    else {
                        currentUser.follows + uid
                    }
                transition.update(
                    usersCollection.document(currentUid), "follows", newFollows
                )
            }.await()
            Resource.Success(!isFollowing)
        }
    }

    override suspend fun getPostForFollows() = withContext(Dispatchers.IO){
        safeCall {
            val uid = auth.uid!!
            val followsList = getUserProfile(uid).data!!.follows
            val allPosts = postsCollection.whereIn("authorUid",followsList)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
                .onEach { post ->
                    val user = getUserProfile(post.authorUid).data!!
                    post.username = user.username
                    post.userProfilePic = user.profilePicUrl
                    post.isLiked = uid in post.likedBy
                }
            Resource.Success(allPosts)
        }
    }

}