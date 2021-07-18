package com.riyazuddin.zing.repositories.network.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Followers
import com.riyazuddin.zing.data.entities.Following
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.CHAT_MESSAGE_PAGE_LIMIT
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.NAME
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import kotlinx.coroutines.tasks.await

class NewChatPagingSource(
    private val uid: String,
    private val firestore: FirebaseFirestore
) : PagingSource<QuerySnapshot, User>() {

    companion object {
        const val TAG = "NewChatPagingSource"
    }

    private var isFollowingLoading = true
    private val hashSet = HashSet<String>()

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        try {
            val result = mutableListOf<User>()

            val currentPage = params.key ?: firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(FOLLOWING_COLLECTION)
                .orderBy("since", Query.Direction.DESCENDING)
                .limit(CHAT_MESSAGE_PAGE_LIMIT)
                .get()
                .await()

            if (isFollowingLoading) {
                currentPage.toObjects(Following::class.java).forEach {
                    if (!hashSet.contains(it.followingToUid)){
                        val user = firestore.collection(USERS_COLLECTION).document(it.followingToUid)
                            .get().await().toObject(User::class.java)!!
                        result.add(user)
                        hashSet.add(it.followingToUid)
                    }
                }
            }else{
                currentPage.toObjects(Followers::class.java).forEach{
                    if (!hashSet.contains(it.followedByUid)) {
                        val user = firestore.collection(USERS_COLLECTION).document(it.followedByUid)
                            .get().await().toObject(User::class.java)!!
                        result.add(user)
                        hashSet.add(it.followedByUid)
                    }
                }
            }

            if (isFollowingLoading && currentPage.size() <= 0){
                isFollowingLoading = false
                val queryForFollowers = firestore
                    .collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(FOLLOWERS_COLLECTION)
                    .orderBy("since", Query.Direction.DESCENDING)
                    .limit(CHAT_MESSAGE_PAGE_LIMIT)
                    .get()
                    .await()
                return LoadResult.Page(result, null, queryForFollowers)
            }
            if (!isFollowingLoading && currentPage.size() <= 0) {
                return LoadResult.Page(result, null, null)
            }

            val lastDocumentSnapshot = currentPage.documents[currentPage.size() - 1]
            val nextQuery = firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(FOLLOWING_COLLECTION)
                .orderBy("since", Query.Direction.DESCENDING)
                .startAfter(lastDocumentSnapshot)
                .limit(CHAT_MESSAGE_PAGE_LIMIT)
                .get()
                .await()

            return LoadResult.Page(result, null, nextQuery)

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}