package com.riyazuddin.zing.repositories.network.implementation

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.network.abstraction.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun register(email: String, password: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user!!.sendEmailVerification()
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