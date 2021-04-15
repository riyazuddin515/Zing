package com.riyazuddin.zing.repositories

import android.net.Uri
import com.algolia.search.model.response.ResponseSearch
import com.riyazuddin.zing.Auth
import com.riyazuddin.zing.data.entities.*
import com.riyazuddin.zing.other.Resource
import java.util.*

class FakeMainRepository : MainRepository {

    //consider current logged in user ad auth1 with
    //corresponding data of user1
    private val auth = mutableListOf<Auth>()

    private val usersCollection = mutableListOf<User>()
    private val postsCollection = mutableListOf<Post>()
    private val postLikesCollection = mutableListOf<PostLikes>()
    private val updateProfileCollection = mutableListOf<UpdateProfile>()
    private val followersCollection = mutableListOf<Followers>()
    private val followingCollection = mutableListOf<Following>()
    private val commentCollection = mutableListOf<Comment>()

    //for storing post image url i.e same as post id for testing
    private val storage = mutableListOf<String>()

    companion object {
        val auth1 = Auth("riyazuddin515@gmail.com", "12345678")
        val auth2 = Auth("fayazuddin786a@gmail.com", "12345678")


        val user1 = User("riyazuddin", UUID.randomUUID().toString(), "riyazuddin515")
        val user2 = User("fayazuddin", UUID.randomUUID().toString(), "fayazuddin786a")
    }

    init {
        usersCollection.add(user1)
        usersCollection.add(user2)

        auth.add(auth1)
        auth.add(auth2)
    }

    override suspend fun createPost(imageUri: Uri, caption: String): Resource<Any> {
        val uid = UUID.randomUUID().toString()
        val postID = UUID.randomUUID().toString()
        val post = Post(
            postId = postID,
            postedBy = uid,
            date = System.currentTimeMillis(),
            imageUrl = postID,
            caption = "test"
        )
        postsCollection.add(post)
        val userIndex = usersCollection.indexOf(user1)
        val userUpdate = usersCollection[userIndex]
        userUpdate.postCount++
        usersCollection[userIndex] = userUpdate
        postLikesCollection.add(PostLikes())

        return Resource.Success(Any())
    }

    override suspend fun getUsers(uids: List<String>): Resource<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserProfile(uid: String): Resource<User> {
        val user = usersCollection.filter { it.uid == uid }[0]

        val currentUser = user1
        val currentUserFollowing = followingCollection.filter { it.uid == currentUser.uid }[0]
        user.isFollowing = uid in currentUserFollowing.following
        return Resource.Success(user)
    }

    override suspend fun getPostForProfile(uid: String): Resource<List<Post>> {
        TODO("Not yet implemented")
    }

    override suspend fun toggleLikeForPost(post: Post): Resource<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun deletePost(post: Post): Resource<Post> {
        postsCollection.remove(post)
        storage.remove(post.postId)
        val userIndex = usersCollection.indexOf(user1)
        val userUpdate = usersCollection[userIndex]
        userUpdate.postCount--
        usersCollection[userIndex] = userUpdate
        return Resource.Success(post)
    }

    /**
     * uid in parameter is otherUser uid
     */
    override suspend fun toggleFollowForUser(uid: String): Resource<Boolean> {

        val isFollowing: Boolean

        val currentUserUid = user1.uid

        val otherUserFollowersList = followersCollection
            .filter { it.uid == uid }[0]

        isFollowing = currentUserUid in otherUserFollowersList.followers

        if (isFollowing) {
            val following = followingCollection.filter { it.uid == currentUserUid }[0]
            val index = followingCollection.indexOf(following)
            following.following = following.following - uid
            followingCollection[index] = following


            val followers = followersCollection.filter { it.uid == uid }[0]
            val index1 = followersCollection.indexOf(followers)
            followers.followers = followers.followers - currentUserUid
            followersCollection[index1] = followers

            val currentUser = user1
            val index3 = usersCollection.indexOf(currentUser)
            currentUser.followingCount--
            usersCollection[index3] = currentUser

            val otherUser = usersCollection.filter { it.uid == uid }[0]
            val index4 = usersCollection.indexOf(otherUser)
            otherUser.followersCount--
            usersCollection[index4] = otherUser

        }else{
            val following = followingCollection.filter { it.uid == currentUserUid }[0]
            val index = followingCollection.indexOf(following)
            following.following = following.following + uid
            followingCollection[index] = following


            val followers = followersCollection.filter { it.uid == uid }[0]
            val index1 = followersCollection.indexOf(followers)
            followers.followers = followers.followers + currentUserUid
            followersCollection[index1] = followers

            val currentUser = user1
            val index3 = usersCollection.indexOf(currentUser)
            currentUser.followingCount++
            usersCollection[index3] = currentUser

            val otherUser = usersCollection.filter { it.uid == uid }[0]
            val index4 = usersCollection.indexOf(otherUser)
            otherUser.followersCount++
            usersCollection[index4] = otherUser
        }
        return Resource.Success(!isFollowing)
    }

    override suspend fun getPostForFollows(): Resource<List<Post>> {
        TODO("Not yet implemented")
    }

    override suspend fun createComment(commentText: String, postId: String): Resource<Comment> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostComments(postId: String): Resource<List<Comment>> {
        val comments = mutableListOf<Comment>()
        val c = commentCollection.filter {
            it.postId == postId
        }
        c.forEach {
            val user = getUserProfile(it.commentedBy).data!!
            it.username = user.username
            it.userProfilePic = user.profilePicUrl
            comments.add(it)
        }
        return Resource.Success(comments)
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
        for (user in usersCollection) {
            if (user.username == query)
                return Resource.Success(false)
        }
        return Resource.Success(true)
    }

    override suspend fun verifyAccount(currentPassword: String): Resource<Any> {
        return if(auth1.password == currentPassword)
            Resource.Success("Verification Success")
        else
            Resource.Error("Verification Failed")
    }

    override suspend fun changePassword(newPassword: String): Resource<Any> {
        val authIndex = auth.indexOf(auth1)
        val authUpdated = auth[authIndex]
        authUpdated.password = newPassword
        auth[authIndex] = authUpdated

        return Resource.Success("Password Changed Successfully")
    }

    override suspend fun getFollowersList(uid: String): Resource<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun getFollowing(uid: String): Resource<Following> {
        TODO("Not yet implemented")
    }

    override suspend fun getFollowers(uid: String): Resource<Followers> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostLikes(postId: String): Resource<PostLikes> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostLikedUsers(postId: String): Resource<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun algoliaSearch(searchQuery: String): Resource<ResponseSearch> {
        TODO("Not yet implemented")
    }
}