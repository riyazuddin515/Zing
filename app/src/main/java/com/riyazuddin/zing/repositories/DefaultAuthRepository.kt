package com.riyazuddin.zing.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.data.entities.Followers
import com.riyazuddin.zing.data.entities.Following
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DefaultAuthRepository : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String
    ): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user!!.sendEmailVerification()
                val uid = result.user!!.uid
                val user = User(name, uid, username)
                firestore.collection(USERS_COLLECTION).document(uid).set(user).await()
                firestore.collection(FOLLOWING_COLLECTION).document(uid).set(Following(uid = uid)).await()
                firestore.collection(FOLLOWERS_COLLECTION).document(uid).set(Followers(uid = uid)).await()

                Resource.Success(true)
            }
        }
    }

    override suspend fun login(email: String, password: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            safeCall {
                auth.signInWithEmailAndPassword(email, password).await()
                Resource.Success(true)
            }
        }
    }

    override suspend fun sendPasswordResetLink(email: String): Resource<String> {
        return withContext(Dispatchers.IO) {
            safeCall {
                auth.sendPasswordResetEmail(email).await()
                Resource.Success("Mail Sent")
            }
        }
    }

    override suspend fun searchUsername(query: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result =
                    firestore.collection(USERS_COLLECTION).whereEqualTo("username", query).get()
                        .await()
                if (result.isEmpty)
                    Resource.Success(true)
                else
                    Resource.Success(false)
            }
        }
    }
}