package com.riyazuddin.zing.repositories

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.other.Constants.POSTS_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class DefaultMainRepository : MainRepository {

    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = FirebaseFirestore.getInstance().collection(USERS_COLLECTION)
    private val postsCollection = FirebaseFirestore.getInstance().collection(POSTS_COLLECTION)
    private val storageRef = FirebaseStorage.getInstance().reference

    override suspend fun createPost(imageUri: Uri, caption: String?) = withContext(Dispatchers.IO){
        safeCall {
            val uid = auth.uid!!
            val postID = UUID.randomUUID().toString()
            val postDownloadUrl = storageRef.child("posts/$uid/$postID").putFile(imageUri).await().metadata?.reference?.downloadUrl?.await().toString()
            val post = Post(postID, uid, System.currentTimeMillis(), postDownloadUrl, caption)
            postsCollection.document(postID).set(post).await()
            Resource.Success(Any())
        }
    }

//    override suspend fun updateProfilePic(uidToUpdate: String, uri: Uri) = withContext(Dispatchers.IO){
//        val userProfileStorageRef = Firebase.storage.getReference(uidToUpdate)
//        val user = getUser().data!!
//        if (user.profilePicUrl != DEFAULT_PROFILE_PICTURE_URL){
//            Firebase.storage.getReferenceFromUrl(user.profilePicUrl).delete().await()
//        }
//        userProfileStorageRef.putFile(uri).await().metadata?.reference?.downloadUrl?.await()
//    }
//
//    override suspend fun updateProfile(updateProfile: UpdateProfile) = withContext(Dispatchers.IO) {
//        safeCall {
//            val imageUrl = updateProfile.profilePicUri?.let { uri ->
//                updateProfilePic(updateProfile.uidToUpdate, uri).toString()
//            }
//
//            val map = mutableMapOf(
//                "username" to updateProfile.username,
//                "firstName" to updateProfile.firstName,
//                "lastName" to updateProfile.lastName
//            )
//
//            imageUrl?.let { url ->
//                map["profilePicUrl"] = url
//            }
//            usersCollection.document(updateProfile.uidToUpdate).update(map.toMap()).await()
//            Resource.Success(Any())
//        }
//    }

}