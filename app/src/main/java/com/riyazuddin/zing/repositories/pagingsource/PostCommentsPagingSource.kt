package com.riyazuddin.zing.repositories.pagingsource

import android.util.Log
import androidx.paging.LoadState
import androidx.paging.PagingSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.data.entities.Comment
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.COMMENT_PAGE_SIZE
import com.riyazuddin.zing.other.Constants.DATE
import kotlinx.coroutines.tasks.await

class PostCommentsPagingSource(
    private val postCommentsCollectionReference: CollectionReference,
    private val usersCollectionReference: CollectionReference
) : PagingSource<QuerySnapshot, Comment>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Comment> {
        return try {

            val currentPage = params.key ?: postCommentsCollectionReference
                .orderBy(DATE, Query.Direction.DESCENDING)
                .limit(COMMENT_PAGE_SIZE.toLong())
                .get()
                .await()

            val lastDocument = currentPage.documents[currentPage.size() - 1]
            Log.i(TAG, "load: ${currentPage.size()}")

            val nextPage = postCommentsCollectionReference
                .orderBy(DATE, Query.Direction.DESCENDING)
                .startAfter(lastDocument)
                .limit(COMMENT_PAGE_SIZE.toLong())
                .get()
                .await()

            LoadResult.Page(
                currentPage.toObjects(Comment::class.java).onEach {
                    val user = usersCollectionReference.document(it.commentedBy).get().await().toObject(User::class.java)!!
                    it.username = user.username
                    it.userProfilePic = user.profilePicUrl
                },
                null,
                nextPage
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    companion object{
        const val TAG = "PostCommentSource"
    }
}