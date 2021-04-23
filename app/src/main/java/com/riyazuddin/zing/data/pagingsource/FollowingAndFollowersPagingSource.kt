package com.riyazuddin.zing.data.pagingsource

import android.nfc.Tag
import android.util.Log
import androidx.paging.PagingSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Followers
import com.riyazuddin.zing.data.entities.Following
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import kotlinx.coroutines.tasks.await

class FollowingAndFollowersPagingSource(private val uid: String): PagingSource<QuerySnapshot, User>() {

    private var isFirst = true
    private var list = setOf<String>()

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        try {
            if(isFirst){
                val following = FirebaseFirestore.getInstance()
                    .collection(FOLLOWING_COLLECTION)
                    .document(uid)
                    .get()
                    .await()
                    .toObject(Following::class.java)!!
                
                following.following.forEach {
                    Log.i(TAG, "load following --> : $it")
                }

                val followers = FirebaseFirestore.getInstance()
                    .collection(FOLLOWERS_COLLECTION)
                    .document(uid)
                    .get()
                    .await()
                    .toObject(Followers::class.java)!!

                list = following.following.union(followers.followers)
                isFirst = false
            }

            val chunks = list.chunked(10)
            val resultList = mutableListOf<User>()
            var currentPage = params.key
            chunks.forEach { chunk ->
                Log.i(TAG, "load: chunk --> $chunk")
                currentPage = params.key ?: FirebaseFirestore
                    .getInstance()
                    .collection(USERS_COLLECTION)
                    .whereIn("uid", chunk)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .get()
                    .await()

                resultList.addAll(currentPage!!.toObjects(User::class.java))
            }


            val lastDocumentSnapshot = currentPage!!.documents[currentPage!!.size() - 1]

            val nextQuery = FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .whereIn("uid", if (chunks.isNotEmpty()) chunks[0] else listOf())
                .orderBy("name", Query.Direction.ASCENDING)
                .startAfter(lastDocumentSnapshot)
                .get()
                .await()

            return LoadResult.Page(
                resultList,
                null,
                nextQuery
            )

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
    
    companion object{
        const val TAG = "FAFPS"
    }
}