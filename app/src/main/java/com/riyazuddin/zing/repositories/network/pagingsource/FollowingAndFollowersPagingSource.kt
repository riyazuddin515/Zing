package com.riyazuddin.zing.repositories.network.pagingsource

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
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.NavGraphArgsConstants.FOLLOWING_ARG
import kotlinx.coroutines.tasks.await

class FollowingAndFollowersPagingSource(
    private val uid: String,
    private val type: String,
    private val firestore: FirebaseFirestore
) : PagingSource<QuerySnapshot, User>() {

    companion object {
        const val TAG = "FAFPS"
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        try {
            val result = mutableListOf<User>()

            val currentPage = params.key ?: firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(if (type == FOLLOWING_ARG) FOLLOWING_COLLECTION else FOLLOWERS_COLLECTION)
                .orderBy("since", Query.Direction.ASCENDING)
                .get()
                .await()

            if (currentPage.size() <= 0)
                return LoadResult.Page(result, null, null)

            if (type == FOLLOWING_ARG) {
                currentPage.toObjects(Following::class.java).forEach {
                    val user = firestore.collection(USERS_COLLECTION).document(it.followingToUid)
                        .get().await().toObject(User::class.java)!!
                    result.add(user)
                }
            } else {
                currentPage.toObjects(Followers::class.java).forEach {
                    val user = firestore.collection(USERS_COLLECTION).document(it.followedByUid)
                        .get().await().toObject(User::class.java)!!
                    result.add(user)
                }
            }
            val lastDocumentSnapshot = currentPage.documents[currentPage.size() - 1]

            val nextQuery = firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(if (type == FOLLOWING_ARG) FOLLOWING_COLLECTION else FOLLOWERS_COLLECTION)
                .orderBy("since", Query.Direction.ASCENDING)
                .startAfter(lastDocumentSnapshot)
                .get()
                .await()

            return LoadResult.Page(result, null, nextQuery)

        } catch (e: Exception) {
            Log.e(TAG, "load: ", e)
            return LoadResult.Error(e)
        }
    }
}