package com.riyazuddin.zing.repositories.network.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.Feed
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.PostLikes
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.DATE
import com.riyazuddin.zing.other.Constants.FEEDS_COLLECTION
import com.riyazuddin.zing.other.Constants.FEED_COLLECTION
import com.riyazuddin.zing.other.Constants.FEED_PAGE_SIZE
import com.riyazuddin.zing.other.Constants.POSTS_COLLECTION
import com.riyazuddin.zing.other.Constants.POST_LIKES_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import kotlinx.coroutines.tasks.await

/**
 * This class is use for paginating the feed
 * i.e; paginating the current user following users posts *
 */
class FeedPagingSource(
    private val db: FirebaseFirestore,
) : PagingSource<QuerySnapshot, Post>() {

    companion object {
        const val TAG = "FeedPagingSource"
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        try {
            val uid =
                Firebase.auth.uid ?: return LoadResult.Error(IllegalStateException("Auth Error"))

            val resultList = mutableListOf<Post>()

            val currentPage = params.key ?: db.collection(FEEDS_COLLECTION)
                .document(uid)
                .collection(FEED_COLLECTION)
                .orderBy(DATE, Query.Direction.DESCENDING)
                .limit(FEED_PAGE_SIZE.toLong())
                .get()
                .await()

            Log.i(TAG, "load: ${currentPage.size()}")

            if (currentPage.size() <= 0)
                return LoadResult.Page(resultList, null, null)

            currentPage.toObjects(Feed::class.java).forEach { feed ->
                val postDocumentSnapshot = db.collection(POSTS_COLLECTION)
                    .document(feed.postId)
                    .get().await()
                if (postDocumentSnapshot.exists()){
                    val post = postDocumentSnapshot.toObject(Post::class.java)!!
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
                    post.isLiked = uid in likesList
                    resultList.add(post)
                }
            }

            val lastDocument = currentPage.documents[currentPage.size() - 1]
            val nextPage = db.collection(FEEDS_COLLECTION)
                .document(uid)
                .collection(FEED_COLLECTION)
                .orderBy(DATE, Query.Direction.DESCENDING)
                .startAfter(lastDocument)
                .limit(FEED_PAGE_SIZE.toLong())
                .get()
                .await()

            return LoadResult.Page(resultList, null, nextPage)

        } catch (e: Exception) {
            Log.e(TAG, "load: ", e)
            return LoadResult.Error(e)
        }
    }
}