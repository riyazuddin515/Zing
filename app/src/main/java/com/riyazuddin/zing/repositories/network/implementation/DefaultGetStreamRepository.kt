package com.riyazuddin.zing.repositories.network.implementation

import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.riyazuddin.zing.di.EncryptedSharedPreferencesAnnotated
import com.riyazuddin.zing.other.Constants.NO_TOKEN
import com.riyazuddin.zing.other.Constants.STREAM_TOKEN_KEY
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
    private val getStreamTokenApi: GetStreamTokenApi,
    @EncryptedSharedPreferencesAnnotated private val sharedPreferences: SharedPreferences
) : GetStreamRepository {

    companion object {
        const val TAG = "DefaultGetStreamRepo"
    }

    override suspend fun getToken(userId: String): Resource<String> =
        withContext(Dispatchers.IO + NonCancellable) {
            safeCall {
                var token = sharedPreferences.getString(STREAM_TOKEN_KEY, NO_TOKEN)
                if (token == null || token == NO_TOKEN) {
                    val response = getStreamTokenApi.getToken(userId)
                    if (response.isSuccessful && response.body() != null) {
                        token = response.body()!!.token
                        sharedPreferences.edit().putString(STREAM_TOKEN_KEY, token)
                            .apply()
                        return@withContext Resource.Success(token)
                    } else {
                        return@withContext Resource.Success(token ?: NO_TOKEN)
                    }
                } else {
                    Log.i(TAG, "getToken: else")
                    return@withContext Resource.Success(token)
                }
            }
        }

    override suspend fun connectUser(user: User): Resource<User> =
        withContext(Dispatchers.IO + NonCancellable) {
            safeCall {
                val token = getToken(user.id)
                if (token.data != null && token.data != NO_TOKEN) {
                    chatClient.getCurrentUser()?.let {
                        return@withContext Resource.Success(it)
                    }
                    val result = chatClient.connectUser(user, token.data).await()
                    if (result.isSuccess) {
                        Resource.Success(result.data().user)
                    } else
                        Resource.Error(result.error().message ?: "Unknown Error")
                } else {
                    Resource.Error("Token response failed" + token.message)
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