package com.riyazuddin.zing.repositories.pagingsource

import android.util.Log
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
    private lateinit var list: List<String>

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        try {
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

                list = following + followers

                Log.i(TAG, "load: $list")
                isFirst = false
            }

            val chunks = list.chunked(10)
            var currentPage = params.key
            chunks.forEach { chunk ->
                currentPage = params.key ?: firestore
                    .collection(USERS_COLLECTION)
                    .whereIn(UID, chunk)
                    .orderBy(NAME, Query.Direction.ASCENDING)
                    .get()
                    .await()
            }

            val lastDocumentSnapshot = currentPage!!.documents[currentPage!!.size() - 1]

            val nextQuery = firestore
                .collection(USERS_COLLECTION)
                .whereIn(UID, if (chunks.isNotEmpty()) chunks[0] else listOf())
                .orderBy(NAME, Query.Direction.ASCENDING)
                .startAfter(lastDocumentSnapshot)
                .get()
                .await()

            return LoadResult.Page(
                currentPage!!.toObjects(User::class.java),
                null,
                nextQuery
            )

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}