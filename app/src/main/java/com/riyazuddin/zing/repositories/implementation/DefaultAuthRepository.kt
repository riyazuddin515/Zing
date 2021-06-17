package com.riyazuddin.zing.repositories.implementation

import com.algolia.search.client.ClientSearch
import com.algolia.search.dsl.attributesForFaceting
import com.algolia.search.dsl.settings
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.response.ResponseSearch
import com.algolia.search.model.search.Query
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.BuildConfig
import com.riyazuddin.zing.data.entities.Followers
import com.riyazuddin.zing.data.entities.Following
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.abstraction.AuthRepository
import io.ktor.client.features.logging.*
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
                firestore.collection(FOLLOWING_COLLECTION).document(uid).set(Following(uid = uid))
                    .await()
                firestore.collection(FOLLOWERS_COLLECTION).document(uid).set(Followers(uid = uid))
                    .await()

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

    override suspend fun algoliaUsernameSearch(searchQuery: String): Resource<ResponseSearch> =
        withContext(Dispatchers.IO) {
            safeCall {
                val client = ClientSearch(
                    ApplicationID(BuildConfig.ALGOLIA_APP_ID),
                    APIKey(BuildConfig.ALGOLIA_SEARCH_KEY),
                    LogLevel.ALL
                )
                val settings = settings {
                    attributesForFaceting {
                        +"username" // or FilterOnly(username) for filtering purposes only
                    }
                }

                val index = client.initIndex(IndexName("user_search"))
                index.setSettings(settings)

                val queryObj = Query(searchQuery)
                val result = index.search(queryObj)
                Resource.Success(result)
            }
        }
}