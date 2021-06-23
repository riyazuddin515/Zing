package com.riyazuddin.zing.repositories.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.PostLikes
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POSTS_COLLECTION
import com.riyazuddin.zing.other.Constants.POST_LIKES_COLLECTION
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import kotlinx.coroutines.tasks.await

class ProfilePostPagingSource(
    private val db: FirebaseFirestore,
    private val uid: String
) : PagingSource<QuerySnapshot, Post>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {
            val currentPage = params.key ?: db.collection(POSTS_COLLECTION)
                .whereEqualTo("postedBy", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(POST_PAGE_SIZE.toLong())
                .get()
                .await()

            Log.i(TAG, "load: ${currentPage.size()}")

            val lastDocumentSnapshot = currentPage.documents[currentPage.size() - 1]

            val nextPage = db.collection(POSTS_COLLECTION)
                .whereEqualTo("postedBy", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(POST_PAGE_SIZE.toLong())
                .startAfter(lastDocumentSnapshot)
                .get()
                .await()

            return LoadResult.Page(
                currentPage.toObjects(Post::class.java).onEach { post ->
                    val user = db.collection(USERS_COLLECTION)
                        .document(uid).get().await().toObject(User::class.java)!!
                    post.username = user.username
                    post.userProfilePic = user.profilePicUrl
                    val postLikesDocumentSnapshot = db.collection(POST_LIKES_COLLECTION)
                        .document(post.postId).get().await()
                    val likesList =
                    if (postLikesDocumentSnapshot.exists())
                        postLikesDocumentSnapshot.toObject(PostLikes::class.java)?.likedBy ?: listOf()
                    else
                        listOf()

                    post.isLiked = Firebase.auth.uid in likesList
                },
                null,
                nextPage
            )

        } catch (e: Exception) {
            Log.e(TAG, "load: ", e)
            LoadResult.Error(e)
        }
    }

    companion object {
        const val TAG = "ProfilePostPagingSource"
    }
}