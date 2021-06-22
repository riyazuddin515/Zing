package com.riyazuddin.zing.repositories.pagingsource

import androidx.paging.PagingSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Followers
import com.riyazuddin.zing.data.entities.Following
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.NAME
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import kotlinx.coroutines.tasks.await

class FollowingAndFollowersPagingSource(
    private val uid: String,
    private val firestore: FirebaseFirestore
) : PagingSource<QuerySnapshot, User>() {

    companion object {
        const val TAG = "FAFPS"
    }

    private var isFirst = true
    private lateinit var list: MutableList<List<String>>

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        try {
            val result = mutableListOf<User>()
            if (isFirst) {
                val following = firestore
                    .collection(FOLLOWING_COLLECTION)
                    .document(uid)
                    .get()
                    .await()
                    .toObject(Following::class.java)?.following ?: listOf()

                val followers = firestore
                    .collection(FOLLOWERS_COLLECTION)
                    .document(uid)
                    .get()
                    .await()
                    .toObject(Followers::class.java)?.followers ?: listOf()

                list = (following + followers).chunked(10).toMutableList()
                isFirst = false
                if (list.isEmpty())
                    return LoadResult.Page(result, null, null)
            }

            val currentPage = params.key ?: firestore
                .collection(USERS_COLLECTION)
                .whereIn(UID, list.removeFirst())
                .orderBy(NAME, Query.Direction.ASCENDING)
                .get()
                .await()
            result.addAll(currentPage.toObjects(User::class.java))

            if (list.isEmpty())
                return LoadResult.Page(result, null, null)

            val nextQuery = firestore
                .collection(USERS_COLLECTION)
                .whereIn(UID, list.removeFirst())
                .orderBy(NAME, Query.Direction.ASCENDING)
                .get()
                .await()

            return LoadResult.Page(result, null, nextQuery)

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}