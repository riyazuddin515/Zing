package com.riyazuddin.zing.services

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.riyazuddin.zing.data.entities.Notification
import com.riyazuddin.zing.di.EncryptedSharedPreferencesAnnotated
import com.riyazuddin.zing.di.SharedPreferencesAnnotated
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.DIRECT_REPLY
import com.riyazuddin.zing.other.Constants.NAME
import com.riyazuddin.zing.other.Constants.NO_TOKEN
import com.riyazuddin.zing.other.Constants.PROFILE_PIC_URL
import com.riyazuddin.zing.other.Constants.STREAM_TOKEN_KEY
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.workers.NotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.Message
import serializeToMap
import toDataClass
import javax.inject.Inject

@AndroidEntryPoint
class DirectReplyReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "DirectReplyReceiverLog"
    }

    @Inject
    lateinit var chatClient: ChatClient

    @Inject
    @EncryptedSharedPreferencesAnnotated
    lateinit var sharedPreferencesEnc: SharedPreferences

    @Inject
    @SharedPreferencesAnnotated
    lateinit var sharedPreferences: SharedPreferences

    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle = RemoteInput.getResultsFromIntent(intent)
        if (bundle != null) {
            val reply = bundle.getCharSequence(DIRECT_REPLY).toString().trim()
            val curUser = chatClient.getCurrentUser()
            val token = sharedPreferencesEnc.getString(STREAM_TOKEN_KEY, NO_TOKEN)
            if (curUser != null) {
                Log.i(TAG, "onReceive: User Not NUll")
                if (token != null && token != NO_TOKEN) {
                    sendMessageAndUpdateNotification(context, reply, intent!!)
                } else
                    Log.i(TAG, "onReceive: Token Null")
            } else if (token != null && token != NO_TOKEN)
                connectUser(token, context, reply, intent!!)
        }
    }

    private fun connectUser(token: String, context: Context?, reply: String, intent: Intent?) {
        val user = io.getstream.chat.android.client.models.User(
            id = sharedPreferences.getString(UID, UID)!!,
            extraData = mutableMapOf(
                "name" to sharedPreferences.getString(NAME, NAME)!!,
                "image" to sharedPreferences.getString(PROFILE_PIC_URL, PROFILE_PIC_URL)!!,
            )
        )

        chatClient.connectUser(user, token).enqueue { connectionResult ->
            if (connectionResult.isSuccess) {
                sendMessageAndUpdateNotification(context, reply, intent)
            } else {
                Log.i(TAG, "onReceive: ${connectionResult.error().message}")
                Log.i(TAG, "onReceive: ${sharedPreferences.getString(UID, UID)!!}")
                Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessageAndUpdateNotification(
        context: Context?,
        reply: String,
        intent: Intent?
    ) {
        try {
            val notificationObj = intent?.extras?.getSerializable("notificationObj")?.serializeToMap()
                    ?.toDataClass<Notification>()

            if (notificationObj != null) {
                val channelClient = chatClient.channel(notificationObj.cid)
                val user = io.getstream.chat.android.client.models.User(
                    id = sharedPreferences.getString(UID, UID)!!,
                    extraData = mutableMapOf(
                        "name" to sharedPreferences.getString(NAME, NAME)!!,
                        "image" to sharedPreferences.getString(PROFILE_PIC_URL, PROFILE_PIC_URL)!!,
                    )
                )
                val message = Message(text = reply, user = user)
                channelClient.sendMessage(message).enqueue { result ->
                    if (result.isSuccess) {
                        //val sentMessage: Message = result.data()
                        //updated value get from bundle

                        channelClient.markRead().enqueue()
                        notificationObj.body = "${notificationObj.body}\nYou: $reply"

                        val map = notificationObj.serializeToMap().toMutableMap()
                        map[Constants.TYPE] = "CHAT_TYPE"
                        map[Constants.KEY] = notificationObj.cid

                        val data = Data.Builder()
                        data.putAll(map)

                        WorkManager.getInstance(context!!).enqueue(
                            OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                                .setInputData(data.build()).build()
                        )

                    } else {
                        // Handle result.error()
                        Log.i(TAG, "sendMessageAndUpdateNotification: ${result.error().message}")
                        Toast.makeText(context, "Message not Sent", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.i(TAG, "sendMessageAndUpdateNotification: notificationObj -> $notificationObj")
            }

        } catch (e: Exception) {
            Log.e(TAG, "sendMessageAndUpdateNotification: ${e.localizedMessage}", e.cause)
            Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show()
        }
    }

}