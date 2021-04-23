package com.riyazuddin.zing.repositories

import android.net.Uri
import com.algolia.search.model.response.ResponseSearch
import com.riyazuddin.zing.Auth
import com.riyazuddin.zing.data.entities.*
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.MainRepository
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FakeMainRepository: MainRepository {

    private val auth = HashMap<String, Auth>()
    private val usersCollection = HashMap<String, User>()
    private val followingCollection = HashMap<String, Following>()
    private val followersCollection = HashMap<String, Followers>()
    private val postCollection = HashMap<String, Post>()
    private val postLikeCollection = HashMap<String, PostLikes>()

    private val storage = HashMap<String, ArrayList<String>>()

    //used as currentUserID
    private val uid1 = UUID.randomUUID().toString()

    init {


        val uid2 = UUID.randomUUID().toString()

        auth[uid1] = Auth("riyazuddin515@gmail.com", "12345678")
        auth[uid2] = Auth("fayazuddin786a@gmail.com","12345678")

        val user1 = User(name = "riyazuddin",uid1, "riyazuddin515")
        val user2 = User(name = "fayazuddin",uid2, "fayazuddin786")

        usersCollection[uid1] = user1
        usersCollection[uid2] = user2
    }

    override suspend fun createPost(imageUri: Uri, caption: String): Resource<Any> {
        val currentUserId = uid1
        val postId = UUID.randomUUID().toString()
        storage[currentUserId]?.add(imageUri.toString())
        val post = Post(postId, currentUserId ,System.currentTimeMillis(), imageUri.toString(), caption)
        postCollection[postId] = post
        val user = usersCollection[currentUserId]!!
        user.postCount++
        usersCollection[currentUserId] = user
        postLikeCollection[postId] = PostLikes()

        return Resource.Success(Any())
    }

    override suspend fun getUsers(uids: List<String>): Resource<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserProfile(uid: String): Resource<User> {
        return Resource.Success(usersCollection[uid]!!)
    }

    override suspend fun getPostForProfile(uid: String): Resource<List<Post>> {
        val posts = postCollection.values.filter {
            it.postedBy == uid
        }
        posts.forEach { post ->
            val user = getUserProfile(post.postId).data!!
            post.username = user.username
            post.userProfilePic = user.profilePicUrl
            post.isLiked = uid in getPostLikes(post.postId).data!!.likedBy
        }

        return Resource.Success(posts)
    }

    override suspend fun toggleLikeForPost(post: Post): Resource<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun deletePost(post: Post): Resource<Post> {
        TODO("Not yet implemented")
    }

    override suspend fun toggleFollowForUser(uid: String): Resource<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostForFollows(): Resource<List<Post>> {
        TODO("Not yet implemented")
    }

    override suspend fun createComment(commentText: String, postId: String): Resource<Comment> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostComments(postId: String): Resource<List<Comment>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateProfile(
        updateProfile: UpdateProfile,
        imageUri: Uri?
    ): Resource<Any> {
        TODO("Not yet implemented")
    }

    override suspend fun updateProfilePic(uid: String, imageUri: Uri): String {
        TODO("Not yet implemented")
    }

    override suspend fun searchUsername(query: String): Resource<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun verifyAccount(currentPassword: String): Resource<Any> {
        TODO("Not yet implemented")
    }

    override suspend fun changePassword(newPassword: String): Resource<Any> {
        TODO("Not yet implemented")
    }

    override suspend fun getFollowersList(uid: String): Resource<Followers> {
        TODO("Not yet implemented")
    }

    override suspend fun getFollowingList(uid: String): Resource<Following> {
        TODO("Not yet implemented")
    }

    override suspend fun getFollowing(uid: String): Resource<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun getFollowers(uid: String): Resource<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostLikes(postId: String): Resource<PostLikes> {
        return Resource.Success(postLikeCollection[postId]!!)
    }

    override suspend fun getPostLikedUsers(postId: String): Resource<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun algoliaSearch(searchQuery: String): Resource<ResponseSearch> {
        TODO("Not yet implemented")
    }
}