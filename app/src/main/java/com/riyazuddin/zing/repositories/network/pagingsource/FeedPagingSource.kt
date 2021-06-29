package com.riyazuddin.zing.repositories.network.pagingsource

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
            val uid =
                Firebase.auth.uid ?: return LoadResult.Error(IllegalStateException("Auth Error"))
            val resultList = mutableListOf<Post>()
            if (isFirstLoad) {
                followingList = db.collection(FOLLOWING_COLLECTION)
                    .document(uid).get().await().toObject(Following::class.java)
                    ?.following ?: listOf()
                isFirstLoad = false
                if (followingList.isEmpty())
                    return LoadResult.Page(resultList, null, null)
            }

            val chunks = followingList.chunked(10)
            var currentPage = params.key
            chunks.forEach { chunk ->
                currentPage = params.key ?: db.collection(POSTS_COLLECTION)
                    .whereIn(POSTED_BY, chunk)
                    .orderBy(DATE, Query.Direction.DESCENDING)
                    .limit(POST_PAGE_SIZE.toLong())
                    .get()
                    .await()

                val parsedPage = currentPage!!.toObjects(Post::class.java)
                    .onEach { post ->
                        val user = db.collection(USERS_COLLECTION)
                            .document(post.postedBy)
                            .get()
                            .await().toObject(User::class.java)!!
                        post.username = user.username
                        post.userProfilePic = user.profilePicUrl

                        val postLikesDocumentSnapshot = db.collection(POST_LIKES_COLLECTION)
                            .document(post.postId).get().await()
                        val likesList = if (!postLikesDocumentSnapshot.exists())
                            listOf()
                        else
                            postLikesDocumentSnapshot.toObject(PostLikes::class.java)?.likedBy
                                ?: listOf()

                        post.isLiked = Firebase.auth.uid in likesList
                    }
                resultList.addAll(parsedPage)
            }

            if (currentPage?.isEmpty == true)
                return LoadResult.Page(resultList, null, null)

            val lastDocumentSnapshot = currentPage!!.documents[currentPage!!.size() - 1]

            val nextPage = db.collection(POSTS_COLLECTION)
                .whereIn(POSTED_BY, if (chunks.isNotEmpty()) chunks[0] else listOf())
                .orderBy(DATE, Query.Direction.DESCENDING)
                .limit(POST_PAGE_SIZE.toLong())
                .startAfter(lastDocumentSnapshot)
                .get()
                .await()

            return LoadResult.Page(resultList, null, nextPage)

        } catch (e: Exception) {
            Log.e(TAG, "load: ", e)
            LoadResult.Error(e)
        }
    }

    companion object {
        const val TAG = "FeedPagingSource"
    }
}