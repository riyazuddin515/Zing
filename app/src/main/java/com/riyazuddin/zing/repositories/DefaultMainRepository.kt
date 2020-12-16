package com.riyazuddin.zing.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DefaultMainRepository : MainRepository {

    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = FirebaseFirestore.getInstance().collection(USERS_COLLECTION)

    override suspend fun getUser(): Resource<User> {
        return withContext(Dispatchers.IO){
            safeCall {
                val uid = auth.uid!!
                val user = usersCollection.document(uid).get().await().toObject(User::class.java)
                Resource.Success(user!!)
            }
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