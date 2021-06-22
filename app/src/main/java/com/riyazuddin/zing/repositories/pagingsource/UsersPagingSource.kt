package com.riyazuddin.zing.repositories.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Followers
import com.riyazuddin.zing.data.entities.Following
import com.riyazuddin.zing.data.entities.PostLikes
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.NAME
import com.riyazuddin.zing.other.Constants.POST_LIKES_COLLECTION
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_LIST_SIZE
import com.riyazuddin.zing.other.NavGraphArgsConstants.FOLLOWERS_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.FOLLOWING_ARG
import com.riyazuddin.zing.other.NavGraphArgsConstants.LIKED_BY_ARG
import kotlinx.coroutines.tasks.await

class UsersPagingSource(
    private val uid: String,
    private val title: String,
    private val firestore: FirebaseFirestore,
) : PagingSource<QuerySnapshot, User>() {

    private var isFirstLoad = true
    private lateinit var list: MutableList<List<String>>

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        return try {
            val result = mutableListOf<User>()
            if (isFirstLoad) {
                list = when (title) {
                    FOLLOWERS_ARG -> {
                        firestore.collection(FOLLOWERS_COLLECTION)
                            .document(uid).get().await().toObject(Followers::class.java)
                            ?.followers?.chunked(USERS_LIST_SIZE)?.toMutableList() ?: mutableListOf()
                    }
                    FOLLOWING_ARG -> {
                        firestore.collection(FOLLOWING_COLLECTION)
                            .document(uid).get().await().toObject(Following::class.java)
                            ?.following?.chunked(USERS_LIST_SIZE)?.toMutableList() ?: mutableListOf()
                    }
                    LIKED_BY_ARG -> {
                        firestore.collection(POST_LIKES_COLLECTION)
                            .document(uid).get().await().toObject(PostLikes::class.java)
                            ?.likedBy?.chunked(USERS_LIST_SIZE)?.toMutableList() ?: mutableListOf()
                    }
                    else -> {
                        mutableListOf()
                    }
                }
                isFirstLoad = false
                if (list.isEmpty())
                    return LoadResult.Page(result, null, null)
            }

            val currentPage = params.key ?: firestore.collection(USERS_COLLECTION)
                .whereIn(UID, list.removeFirst())
                .orderBy(NAME, Query.Direction.ASCENDING)
                .get()
                .await()
            result.addAll(currentPage!!.toObjects(User::class.java))

            if (list.isEmpty())
                return LoadResult.Page(result, null, null)

            val nextPage = firestore.collection(USERS_COLLECTION)
                .whereIn(UID, list.removeFirst())
                .orderBy(NAME, Query.Direction.ASCENDING)
                .get()
                .await()

            return LoadResult.Page(result, null, nextPage)

        } catch (e: Exception) {
            Log.e(TAG, "load: ", e)
            LoadResult.Error(e)
        }
    }

    companion object {
        const val TAG = "UsersListPagingSource"
    }
}