package com.riyazuddin.zing.repositories.network.implementation

import android.net.Uri
import android.util.Log
import com.algolia.search.client.ClientSearch
import com.algolia.search.dsl.attributesForFaceting
import com.algolia.search.dsl.settings
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.response.ResponseSearch
import com.algolia.search.model.search.Query
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.BuildConfig
import com.riyazuddin.zing.data.entities.*
import com.riyazuddin.zing.other.Constants.ALGOLIA_USER_SEARCH_INDEX
import com.riyazuddin.zing.other.Constants.BIO
import com.riyazuddin.zing.other.Constants.COMMENTS_COLLECTION
import com.riyazuddin.zing.other.Constants.DEFAULT_PROFILE_PICTURE_URL
import com.riyazuddin.zing.other.Constants.FOLLOWERS
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWERS_COUNT
import com.riyazuddin.zing.other.Constants.FOLLOWER_REQUESTS_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING
import com.riyazuddin.zing.other.Constants.FOLLOWING_COLLECTION
import com.riyazuddin.zing.other.Constants.FOLLOWING_COUNT
import com.riyazuddin.zing.other.Constants.FOLLOWING_REQUESTS_COLLECTION
import com.riyazuddin.zing.other.Constants.LAST_SEEN
import com.riyazuddin.zing.other.Constants.LIKED_BY
import com.riyazuddin.zing.other.Constants.LIKE_COUNT
import com.riyazuddin.zing.other.Constants.NAME
import com.riyazuddin.zing.other.Constants.NO_USER_DOCUMENT
import com.riyazuddin.zing.other.Constants.OFFLINE
import com.riyazuddin.zing.other.Constants.ONLINE
import com.riyazuddin.zing.other.Constants.POSTS_COLLECTION
import com.riyazuddin.zing.other.Constants.POST_COUNT
import com.riyazuddin.zing.other.Constants.POST_LIKES_COLLECTION
import com.riyazuddin.zing.other.Constants.PRIVACY
import com.riyazuddin.zing.other.Constants.PROFILE_PIC_URL
import com.riyazuddin.zing.other.Constants.REQUESTED_TO_UIDS
import com.riyazuddin.zing.other.Constants.REQUESTED_UIDS
import com.riyazuddin.zing.other.Constants.STATE
import com.riyazuddin.zing.other.Constants.TOKEN
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.other.Constants.USERNAME
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_METADATA_COLLECTION
import com.riyazuddin.zing.other.Constants.USERS_STAT_COLLECTION
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.local.ChatDatabase
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import io.ktor.client.features.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultMainRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val chatDatabase: ChatDatabase,
    private val cloudStorage: FirebaseStorage
) : MainRepository {

    companion object {
        const val TAG = "MainRepository"
    }

    private val usersCollection = firestore.collection(USERS_COLLECTION)
    private val usersMetadataCollection = firestore.collection(USERS_METADATA_COLLECTION)
    private val usersStatCollection = firestore.collection(USERS_STAT_COLLECTION)
    private val postsCollection = firestore.collection(POSTS_COLLECTION)
    private val commentsCollection = firestore.collection(COMMENTS_COLLECTION)

//    private val followingCollection = firestore.collection(FOLLOWING_COLLECTION)
//    private val followersCollection = firestore.collection(FOLLOWERS_COLLECTION)

    private val postLikesCollection = firestore.collection(POST_LIKES_COLLECTION)

    private val followingRequestsCollection = firestore.collection(FOLLOWING_REQUESTS_COLLECTION)
    private val followerRequestsCollection = firestore.collection(FOLLOWER_REQUESTS_COLLECTION)

    override suspend fun onlineOfflineToggleWithDeviceToken(uid: String) {
        withContext(Dispatchers.IO) {
            safeCall {
                val userStatusDatabaseRef =
                    database.reference.child("status/$uid")

                val token = FirebaseMessaging.getInstance().token.await()

                val isOfflineForDatabase = mapOf(
                    STATE to OFFLINE,
                    LAST_SEEN to ServerValue.TIMESTAMP
                )

                val isOnlineForDatabase = mapOf(
                    STATE to ONLINE,
                    LAST_SEEN to ServerValue.TIMESTAMP
                )

                val isOfflineForFirestore = UserStat(state = OFFLINE, token = token, uid = uid)
                val isOnlineForFirestore = UserStat(state = ONLINE, token = token, uid = uid)

                database.getReference(Date().toString()).keepSynced(true)
                database.getReference(".info/connected").addValueEventListener(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value == false) {
                                usersStatCollection.document(uid).set(isOfflineForFirestore, SetOptions.merge())
                                return
                            }

                            userStatusDatabaseRef.onDisconnect().setValue(isOfflineForDatabase)
                                .addOnCompleteListener {
                                    userStatusDatabaseRef.setValue(isOnlineForDatabase)
                                    usersStatCollection.document(uid)
                                        .set(isOnlineForFirestore, SetOptions.merge())
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "onCancelled: ", error.toException())
                        }
                    }
                )
                Resource.Success(Any())
            }
        }
    }

    override suspend fun removeDeviceToken(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            usersStatCollection.document(uid).update(TOKEN, "").await()
            chatDatabase.clearAllTables()
            Resource.Success(true)
        }
    }

    override suspend fun createPost(imageUri: Uri, caption: String) = withContext(Dispatchers.IO) {
        safeCall {
            val uid = auth.uid!!
            val postID = UUID.randomUUID().toString()
            val postDownloadUrl = cloudStorage.reference.child("posts/$uid/$postID").putFile(imageUri)
                .await().metadata?.reference?.downloadUrl?.await().toString()
            firestore.runBatch { batch ->
                val post = Post(postID, uid, postDownloadUrl, caption)
                batch.set(postsCollection.document(postID), post)
                batch.update(
                    usersMetadataCollection.document(uid),
                    POST_COUNT,
                    FieldValue.increment(1)
                )
                batch.set(postLikesCollection.document(postID), PostLikes(uid = uid))
            }.await()
            Resource.Success(Any())
        }
    }

    override suspend fun getPost(postId: String): Resource<Post> = withContext(Dispatchers.IO) {
        safeCall {
            val post = postsCollection.document(postId).get().await().toObject(Post::class.java)
                ?: return@withContext Resource.Error("Post not Found")
            val user =
                usersCollection.document(post.postedBy).get().await().toObject(User::class.java)!!
            post.userProfilePic = user.profilePicUrl
            post.username = user.username
            post.isLiked = user.uid in getPostLikes(postId).data?.likedBy ?: listOf()
            Resource.Success(post)
        }
    }

    override suspend fun getUserProfile(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            val user = usersCollection.document(uid).get().await().toObject(User::class.java)
            user?.let {
                val currentUid = auth.uid!!
                if (uid != currentUid) {
                    val currentUserFollowing = usersCollection.document(currentUid).collection(
                        FOLLOWING_COLLECTION).document(uid).get().await()

                    user.isFollowing = currentUserFollowing.exists()
                }
                Resource.Success(user)
            } ?: Resource.Error(NO_USER_DOCUMENT)
        }
    }

    override suspend fun getPostLikes(postId: String) = withContext(Dispatchers.IO) {
        safeCall {
            val postLikes =
                postLikesCollection.document(postId).get().await().toObject(PostLikes::class.java)
                    ?: PostLikes()
            Resource.Success(postLikes)
        }
    }

    override suspend fun getUsers(uids: List<String>) = withContext(Dispatchers.IO) {
        safeCall {
            val chunks = uids.chunked(10)
            val resultList = mutableListOf<User>()
            chunks.forEach { chunk ->
                val usersList =
                    usersCollection.whereIn(UID, chunk).orderBy(USERNAME).get().await()
                        .toObjects(User::class.java)
                resultList.addAll(usersList)
            }

            Resource.Success(resultList.toList())
        }
    }

    override suspend fun toggleLikeForPost(post: Post) = withContext(Dispatchers.IO) {
        safeCall {
            var isLiked = false
            firestore.runTransaction { transition ->
                val uid = auth.uid!!
                val postDocumentSnapshot = transition.get(postsCollection.document(post.postId))
                if (!postDocumentSnapshot.exists())
                    Resource.Error("Can't access the Post", null)
                val documentSnapshot = transition.get(postLikesCollection.document(post.postId))
                if (!documentSnapshot.exists()) {
                    transition.set(
                        postLikesCollection.document(post.postId),
                        PostLikes(uid = post.postedBy)
                    )
                }

                val currentLikes =
                    documentSnapshot.toObject(PostLikes::class.java)?.likedBy ?: listOf()
                if (uid in currentLikes) {
                    transition.update(
                        postLikesCollection.document(post.postId),
                        LIKED_BY,
                        currentLikes - uid
                    )
                    transition.update(
                        postsCollection.document(post.postId),
                        LIKE_COUNT, FieldValue.increment(-1)
                    )
                } else {
                    isLiked = true
                    transition.update(
                        postLikesCollection.document(post.postId),
                        LIKED_BY,
                        currentLikes + uid
                    )
                    transition.update(
                        postsCollection.document(post.postId),
                        LIKE_COUNT, FieldValue.increment(1)
                    )
                }
            }.await()
            Resource.Success(isLiked)
        }
    }

    override suspend fun deletePost(post: Post) = withContext(Dispatchers.IO) {
        safeCall {
            firestore.runTransaction { transition ->
                transition.delete(commentsCollection.document(post.postId))
                transition.delete(postsCollection.document(post.postId))
                transition.update(usersMetadataCollection.document(post.postedBy), POST_COUNT, FieldValue.increment(-1))
                transition.delete(postLikesCollection.document(post.postId))
            }.await()
            cloudStorage.getReferenceFromUrl(post.imageUrl).delete().await()
            Resource.Success(post)
        }
    }

    override suspend fun toggleFollowForUser(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            var isFollowing = false
            val currentUserUid = auth.uid!!

            firestore.runTransaction { transition ->

                val currentUserFollowingDocumentSnapshot =
                    transition.get(usersCollection.document(currentUserUid).collection(FOLLOWING_COLLECTION).document(uid))

                isFollowing = currentUserFollowingDocumentSnapshot.exists()

                if (isFollowing) {
                    transition.delete(usersCollection.document(currentUserUid).collection(FOLLOWING_COLLECTION).document(uid))
                    transition.delete(usersCollection.document(uid).collection(FOLLOWERS_COLLECTION).document(currentUserUid))
                    transition.update(
                        usersMetadataCollection.document(currentUserUid),
                        FOLLOWING_COUNT,
                        FieldValue.increment(-1)
                    )
                    transition.update(
                        usersMetadataCollection.document(uid),
                        FOLLOWERS_COUNT,
                        FieldValue.increment(-1)
                    )
                    Log.i(TAG, "toggleFollowForUser: UnFollow Success")
                }else{
                    transition.set(usersCollection.document(currentUserUid).collection(FOLLOWING_COLLECTION).document(uid), Following(uid))
                    transition.set(usersCollection.document(uid).collection(FOLLOWERS_COLLECTION).document(currentUserUid), Followers(currentUserUid))
                    transition.update(
                        usersMetadataCollection.document(currentUserUid),
                        FOLLOWING_COUNT,
                        FieldValue.increment(1)
                    )
                    transition.update(
                        usersMetadataCollection.document(uid),
                        FOLLOWERS_COUNT,
                        FieldValue.increment(1)
                    )

                    Log.i(TAG, "toggleFollowForUser: Follow Success")
                }
                null
            }.await()
            Resource.Success(!isFollowing)
        }
    }

    override suspend fun createComment(commentText: String, postId: String) =
        withContext(Dispatchers.IO) {
            safeCall {
                val commentId = UUID.randomUUID().toString()
                val comment =
                    Comment(commentId, commentText, postId, auth.uid!!)

                commentsCollection.document(postId).collection(COMMENTS_COLLECTION)
                    .document(commentId).set(comment).await()
                Resource.Success(comment)
            }
        }

    override suspend fun updateProfile(updateProfile: UpdateProfile, imageUri: Uri?) =
        withContext(Dispatchers.IO) {
            safeCall {
                val imageDownloadUrl = imageUri?.let {
                    val storageRef =
                        cloudStorage.reference.child("profilePics/${updateProfile.uidToUpdate}")
                    if (updateProfile.profilePicUrl != DEFAULT_PROFILE_PICTURE_URL) {
                        cloudStorage.getReferenceFromUrl(updateProfile.profilePicUrl).delete().await()
                    }
                    storageRef.putFile(imageUri).await().metadata?.reference?.downloadUrl?.await()
                        .toString()
                }

                val existing = usersCollection.document(updateProfile.uidToUpdate).get().await()
                if (!existing.exists()) {
                    val uid = updateProfile.uidToUpdate
                    val user = User(name = updateProfile.name, uid, updateProfile.username)
                    imageDownloadUrl?.let {
                        user.profilePicUrl = it
                    }
                    usersCollection.document(updateProfile.uidToUpdate).set(user).await()
                    usersMetadataCollection.document(uid).set(UserMetadata(uid)).await()

                }else{
                    val map = mutableMapOf(
                        NAME to updateProfile.name,
                        USERNAME to updateProfile.username,
                        BIO to updateProfile.bio
                    )
                    imageDownloadUrl?.let {
                        map[PROFILE_PIC_URL] = it
                    }
                    usersCollection.document(updateProfile.uidToUpdate).update(map.toMap()).await()
                }
                Resource.Success(Any())
            }
        }


    override suspend fun verifyAccount(currentPassword: String): Resource<Any> =
        withContext(Dispatchers.IO) {
            safeCall {
                val currentUser = auth.currentUser!!
                val email = currentUser.email.toString()
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                currentUser.reauthenticate(credential).await()
                Resource.Success("Verification Success")
            }
        }

    override suspend fun changePassword(
        newPassword: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        safeCall {
            val currentUser = auth.currentUser!!
            currentUser.updatePassword(newPassword).await()
            Resource.Success("Password Changed Successfully")
        }
    }

    override suspend fun algoliaSearch(searchQuery: String): Resource<ResponseSearch> =
        withContext(Dispatchers.IO) {
            safeCall {
                val client = ClientSearch(
                    ApplicationID(BuildConfig.ALGOLIA_APP_ID),
                    APIKey(BuildConfig.ALGOLIA_SEARCH_KEY),
                    LogLevel.ALL
                )
                val index = client.initIndex(IndexName(ALGOLIA_USER_SEARCH_INDEX))

                val queryObj = Query(searchQuery)
                val result = index.search(queryObj)
                Resource.Success(result)
            }
        }

    override suspend fun deleteComment(comment: Comment): Resource<Comment> =
        withContext(Dispatchers.IO) {
            safeCall {
                commentsCollection
                    .document(comment.postId)
                    .collection(COMMENTS_COLLECTION)
                    .document(comment.commentId)
                    .delete()
                    .await()

                Resource.Success(comment)
            }
        }

//    override suspend fun firebaseUserSearch(query: String) = withContext(Dispatchers.IO) {
//        safeCall {
//            val usersList = usersCollection
//                .orderBy("username")
//                .startAt(query)
//                .endAt(query + "\uf8ff")
//                .get()
//                .await()
//                .toObjects(User::class.java)
//            Resource.Success(usersList)
//        }
//    }

    override suspend fun getUserMetaData(uid: String): Resource<UserMetadata> =
        withContext(Dispatchers.IO) {
            safeCall {
                val userMetadataDocumentSnapshot = firestore.collection(USERS_METADATA_COLLECTION)
                    .document(uid)
                    .get()
                    .await()
                val userMetadata = if (!userMetadataDocumentSnapshot.exists())
                    UserMetadata()
                else
                    userMetadataDocumentSnapshot.toObject(UserMetadata::class.java)!!
                Resource.Success(userMetadata)
            }
        }

    override suspend fun toggleAccountPrivacy(uid: String, privacy: String): Resource<String> =
        withContext(Dispatchers.IO) {
            safeCall {
                firestore.runTransaction { transition ->
                    transition.update(
                        usersCollection.document(uid),
                        PRIVACY, privacy
                    )
                    transition.delete(followingRequestsCollection.document(uid))
                }.await()
                Resource.Success(privacy)
            }
        }

    override suspend fun toggleSendFollowerRequest(uid: String): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            safeCall {
                Log.i(TAG, "toggleSendFollowingRequest: ")
                var sent = false
                val currentUid = Firebase.auth.uid!!
                firestore.runTransaction { transition ->

                    val cur = transition.get(followingRequestsCollection.document(currentUid))
                    val other = transition.get(followerRequestsCollection.document(uid))

                    if (!cur.exists())
                        transition.set(followingRequestsCollection.document(currentUid), FollowingRequest(uid = currentUid))

                    if (!other.exists())
                        transition.set(followerRequestsCollection.document(uid), FollowerRequest(uid = uid))


                    val curList = cur.toObject(FollowingRequest::class.java)?.requestedToUids ?: listOf()
                    sent = uid in curList

                    if (sent) {
                        //already sent
                        transition.update(
                            followingRequestsCollection.document(currentUid),
                            REQUESTED_TO_UIDS,
                            FieldValue.arrayRemove(uid)
                        )
                        transition.update(
                            followerRequestsCollection.document(uid),
                            REQUESTED_UIDS,
                            FieldValue.arrayRemove(currentUid)
                        )
                        Log.i(TAG, "toggleSendFollowerRequest: UndoRequest")
                    } else {
                        //sent now
                        transition.update(
                            followingRequestsCollection.document(currentUid),
                            REQUESTED_TO_UIDS,
                            FieldValue.arrayUnion(uid)
                        )
                        transition.update(
                            followerRequestsCollection.document(uid),
                            REQUESTED_UIDS,
                            FieldValue.arrayUnion(currentUid)
                        )
                        Log.i(TAG, "toggleSendFollowerRequest: sendingRequest")
                    }
                    null
                }.await()
                Resource.Success(!sent)
            }
        }

    override suspend fun checkForPreviousFollowerRequests(uid: String): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            safeCall {
                val documentSnapshot =
                    followingRequestsCollection.document(Firebase.auth.uid!!).get().await()
                if (!documentSnapshot.exists())
                    Resource.Success(false)
                val list = documentSnapshot.toObject(FollowingRequest::class.java)?.requestedToUids
                    ?: listOf()
                Resource.Success(uid in list)
            }
        }

    override suspend fun checkDoesUserHaveFollowerRequests(): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            safeCall {
                val documentSnapshot =
                    followerRequestsCollection.document(Firebase.auth.uid!!).get().await()
                if (!documentSnapshot.exists())
                    Resource.Success(false)
                val list = documentSnapshot.toObject(FollowerRequest::class.java)?.requestedUids
                    ?: listOf()
                Resource.Success(list.isNotEmpty())
            }
        }

    override suspend fun acceptOrRejectTheFollowerRequest(
        uid: String,
        action: Boolean
    ): Resource<String> = withContext(Dispatchers.IO) {
        safeCall {
            val currentUserUid = Firebase.auth.uid!!
            firestore.runTransaction { transition ->

                val a = transition.get(followingRequestsCollection.document(uid))
                val b = transition.get(followerRequestsCollection.document(currentUserUid))

                if (action) {
                    transition.set(
                        usersCollection.document(uid).collection(FOLLOWING_COLLECTION).document(currentUserUid),
                        Following(currentUserUid)
                    )
                    transition.set(
                        usersCollection.document(currentUserUid).collection(FOLLOWERS_COLLECTION).document(uid),
                        Followers(uid)
                    )

                    transition.update(
                        usersMetadataCollection.document(uid),
                        FOLLOWING_COUNT,FieldValue.increment(1)
                    )
                    transition.update(
                        usersMetadataCollection.document(currentUserUid),
                        FOLLOWERS_COUNT,FieldValue.increment(1)
                    )
                }

                if (a.exists())
                    transition.update(followingRequestsCollection.document(uid),REQUESTED_TO_UIDS,FieldValue.arrayRemove(currentUserUid))

                if (b.exists())
                    transition.update(followerRequestsCollection.document(currentUserUid), REQUESTED_UIDS, FieldValue.arrayRemove(uid))

            }.await()
            Resource.Success(uid)
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