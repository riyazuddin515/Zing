package com.riyazuddin.zing.data.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.PostLikes
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.POSTS_COLLECTION
import com.riyazuddin.zing.other.Constants.POST_LIKES_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import kotlinx.coroutines.tasks.await

class ProfilePostPagingSource(
    private val db: FirebaseFirestore,
    private val uid: String
): PagingSource<QuerySnapshot, Post>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {
            val currentPage = params.key ?: db.collection(POSTS_COLLECTION)
                .whereEqualTo("postedBy", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val lastDocumentSnapshot = currentPage.documents[currentPage.size() -1]

            val nextPage = db.collection(POSTS_COLLECTION)
                .whereEqualTo("postedBy", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .startAfter(lastDocumentSnapshot)
                .get()
                .await()

            return LoadResult.Page(
                currentPage.toObjects(Post::class.java).onEach { post ->
                    val user = db.collection(USERS_COLLECTION)
                        .document(uid).get().await().toObject(User::class.java)!!
                    post.username = user.username
                    post.userProfilePic = user.profilePicUrl
                    post.isLiked = uid in db.collection(POST_LIKES_COLLECTION)
                        .document(post.postId).get().await()
                        .toObject(PostLikes::class.java)!!.likedBy
                },
                null,
                nextPage
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}