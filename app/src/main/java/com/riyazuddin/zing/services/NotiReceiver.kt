package com.riyazuddin.zing.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.riyazuddin.zing.R
import com.riyazuddin.zing.di.EncryptedSharedPreferencesAnnotated
import com.riyazuddin.zing.di.SharedPreferencesAnnotated
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.CID
import com.riyazuddin.zing.other.Constants.NOTIFICATION_TAG
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.Message
import javax.inject.Inject

class NotiReceiver: BroadcastReceiver() {

    companion object {
        const val TAG = "NotiReceiverLog"
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
        val tag = intent?.getStringExtra("apple")
        val cid = intent?.getStringExtra(CID)
        Log.i(TAG, "onReceive: tag -> $tag")
        Log.i(TAG, "onReceive: cid -> $cid")
    }

    private fun connectUser(token: String, context: Context?, reply: String, intent: Intent) {
        val user = io.getstream.chat.android.client.models.User(
            id = sharedPreferences.getString(Constants.UID, Constants.UID)!!,
            extraData = mutableMapOf(
                "name" to sharedPreferences.getString(Constants.NAME, Constants.NAME)!!,
                "image" to sharedPreferences.getString(
                    Constants.PROFILE_PIC_URL,
                    Constants.PROFILE_PIC_URL
                )!!,
            )
        )

        chatClient.connectUser(user, token).enqueue { connectionResult ->
            if (connectionResult.isSuccess) {
                sendMessageAndUpdateNotification(context, reply, intent)
            } else {
                Log.i(TAG, "onReceive: ${connectionResult.error().message}")
                Log.i(
                    TAG, "onReceive: ${sharedPreferences.getString(
                        Constants.UID,
                        Constants.UID
                    )!!}")
                Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessageAndUpdateNotification(context: Context?, reply: String, intent: Intent?) {
        try {

            val tag = intent?.extras?.getString(NOTIFICATION_TAG)!!
            val cid = intent.extras?.getString(Constants.CID)!!

            val channelClient = chatClient.channel("messaging", cid)
            val user = io.getstream.chat.android.client.models.User(
                id = sharedPreferences.getString(Constants.UID, Constants.UID)!!,
                extraData = mutableMapOf(
                    "name" to sharedPreferences.getString(Constants.NAME, Constants.NAME)!!,
                    "image" to sharedPreferences.getString(
                        Constants.PROFILE_PIC_URL,
                        Constants.PROFILE_PIC_URL
                    )!!,
                )
            )
            val message = Message(text = reply, user = user)
            channelClient.sendMessage(message).enqueue { result ->
                if (result.isSuccess) {
//                val sentMessage: Message = result.data()
                    //updated value get from bundle
                    val mBuilder =
                        NotificationCompat.Builder(context!!, Constants.CHAT_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification_icon)
                            .setContentTitle("Your Reply: $reply")
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(
                        tag,
                        Constants.NOTIFICATION_ID,
                        mBuilder.build()
                    )
                } else {
                    // Handle result.error()
                    Log.i(TAG, "sendMessageAndUpdateNotification: ${result.error().message}")
                    Toast.makeText(context, "Message not Sent", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "sendMessageAndUpdateNotification: failed")
            Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show()
        }
    }
}