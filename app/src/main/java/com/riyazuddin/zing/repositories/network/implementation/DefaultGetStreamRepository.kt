package com.riyazuddin.zing.repositories.network.implementation

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.other.safeCall
import com.riyazuddin.zing.repositories.network.abstraction.GetStreamRepository
import com.riyazuddin.zing.repositories.network.abstraction.GetStreamTokenApi
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.call.await
import io.getstream.chat.android.client.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGetStreamRepository @Inject constructor(
    private val chatClient: ChatClient,
    private val getStreamTokenApi: GetStreamTokenApi
) : GetStreamRepository {

    companion object {
        const val TAG = "DefaultGetStreamRepo"
    }

    override suspend fun connectUser(user: User): Resource<User> =
        withContext(Dispatchers.IO + NonCancellable) {
            safeCall {
                chatClient.getCurrentUser()?.let {
                    Log.i(TAG, "connectUser: not null")
                    return@safeCall Resource.Success(it)
                }
                Log.i(TAG, "connectUser: null")
                val response = getStreamTokenApi.getToken(user.id)
                if (response.isSuccessful && response.body() != null) {
                    val getStreamToken = response.body()!!
                    val result = chatClient.connectUser(user, getStreamToken.token).await()
                    if (result.isSuccess) {
                        Resource.Success(result.data().user)
                    } else
                        Resource.Error(result.error().message ?: "Unknown Error")
                } else {
                    Resource.Error("Response failed")
                }
            }
        }

    override suspend fun createChatChannel(
        currentUid: String,
        otherEndUserUid: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        safeCall {
            val result = chatClient.createChannel(
                channelType = "messaging",
                members = listOf(currentUid, otherEndUserUid)
            ).await()
            if (result.isSuccess)
                Resource.Success(result.data().cid)
            else
                Resource.Error(result.error().message ?: "Unknown Error")
        }
    }

    override suspend fun setFCMTokenInStream(): Resource<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            val token = FirebaseMessaging.getInstance().token.await()
            val result = chatClient.addDevice(token).await()
            if (result.isSuccess)
                Resource.Success(Unit)
            else
                Resource.Error("Update Failed")
        }
    }


}