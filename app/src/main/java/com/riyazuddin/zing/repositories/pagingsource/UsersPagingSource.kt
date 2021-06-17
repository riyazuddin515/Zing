package com.riyazuddin.zing.repositories.pagingsource

import androidx.paging.PagingSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.NAME
import com.riyazuddin.zing.other.Constants.UID
import kotlinx.coroutines.tasks.await

class UsersPagingSource(
    private val list: List<String>,
    private val collectionReference: CollectionReference
) : PagingSource<QuerySnapshot, User>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        return try {
            val chunks = list.chunked(10)
            var currentPage = params.key
            chunks.forEach { chunk ->
                currentPage = params.key ?: collectionReference
                    .whereIn(UID, chunk)
                    .orderBy(NAME)
                    .limit(10)
                    .get()
                    .await()
            }

            val lastDocumentSnapshot = currentPage!!.documents[currentPage!!.size() - 1]

            val nextPage = collectionReference
                .whereIn(UID, if (chunks.isNotEmpty()) chunks[0] else listOf())
                .orderBy(NAME)
                .startAfter(lastDocumentSnapshot)
                .limit(10)
                .get()
                .await()

            LoadResult.Page(
                currentPage!!.toObjects(User::class.java),
                null,
                nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}