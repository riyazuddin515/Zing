package com.riyazuddin.zing.repositories.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.Following
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.PostLikes
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.DATE
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.POSTED_BY
import com.riyazuddin.zing.other.Constants.POSTS_COLLECTION
import com.riyazuddin.zing.other.Constants.POST_LIKES_COLLECTION
import com.riyazuddin.zing.other.Constants.POST_PAGE_SIZE
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import kotlinx.coroutines.tasks.await

/**
 * This class is use for paginating the feed
 * i.e; paginating the current user following users posts *
 */
class FeedPagingSource(
    private val db: FirebaseFirestore,
) : PagingSource<QuerySnapshot, Post>() {

    private var isFirstLoad = true
    private lateinit var followingList: List<String>

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {
            val uid = Firebase.auth.uid!!

            if (isFirstLoad) {
                followingList = db.collection(FOLLOWING_COLLECTION)
                    .document(uid).get().await().toObject(Following::class.java)
                    ?.following ?: listOf()
                followingList = followingList + uid
                isFirstLoad = false
            }

            val chunks = followingList.chunked(10)
            val resultList = mutableListOf<Post>()
            var currentPage = params.key
            chunks.forEach { chunk ->
                currentPage = params.key ?: db.collection(POSTS_COLLECTION)
                    .whereIn(POSTED_BY, chunk)
                    .orderBy(DATE, Query.Direction.DESCENDING)
                    .limit(POST_PAGE_SIZE.toLong())
                    .get()
                    .await()

                Log.i(TAG, "load: ${currentPage?.size()}")

                val parsedPage = currentPage!!.toObjects(Post::class.java)
                    .onEach { post ->
                        val user = db.collection(USERS_COLLECTION)
                            .document(post.postedBy)
                            .get()
                            .await().toObject(User::class.java)!!
                        post.username = user.username
                        post.userProfilePic = user.profilePicUrl
                        post.isLiked = uid in db.collection(POST_LIKES_COLLECTION)
                            .document(post.postId).get().await()
                            .toObject(PostLikes::class.java)!!.likedBy
                    }
                resultList.addAll(parsedPage)
            }

            Log.i(TAG, "load: result -> ${resultList.size}")

            val lastDocumentSnapshot = currentPage!!.documents[currentPage!!.size() - 1]

            val nextPage = db.collection(POSTS_COLLECTION)
                .whereIn(POSTED_BY, if (chunks.isNotEmpty()) chunks[0] else listOf())
                .orderBy(DATE, Query.Direction.DESCENDING)
                .limit(POST_PAGE_SIZE.toLong())
                .startAfter(lastDocumentSnapshot)
                .get()
                .await()

            resultList.forEach {
                Log.d(TAG, "load: ${it.username}")
            }
            return LoadResult.Page(
                resultList,
                null,
                nextPage
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    companion object {
        const val TAG = "FeedPagingSource"
    }
}