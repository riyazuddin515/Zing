package com.riyazuddin.zing.services

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.riyazuddin.zing.data.Notification
import com.riyazuddin.zing.other.Constants.CHAT_TYPE
import com.riyazuddin.zing.other.Constants.CID
import com.riyazuddin.zing.other.Constants.COMMENT_TYPE
import com.riyazuddin.zing.other.Constants.FOLLOW_TYPE
import com.riyazuddin.zing.other.Constants.KEY
import com.riyazuddin.zing.other.Constants.POST_ID
import com.riyazuddin.zing.other.Constants.POST_LIKE_TYPE
import com.riyazuddin.zing.other.Constants.TYPE
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.workers.NotificationWorker
import io.getstream.chat.android.client.ChatClient
import serializeToMap


class ZingFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: fcm created")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: fcm destroyed")
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseMessaging.getInstance().subscribeToTopic("PUSH_RC")
        Log.i(TAG, "onNewToken: $token")
        try {
            ChatClient.setFirebaseToken(token)
        } catch (e: Exception) {
            Log.e(TAG, "onNewToken: ", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            val notification =
                Gson().fromJson(remoteMessage.data["notification"], Notification::class.java)

            val map = notification.serializeToMap().toMutableMap()
            val type = remoteMessage.data[TYPE]!!
            var key: String? = null
            when (type) {
                CHAT_TYPE -> key = remoteMessage.data[CID]!!
                COMMENT_TYPE -> key = remoteMessage.data[POST_ID]!!
                POST_LIKE_TYPE -> key = remoteMessage.data[POST_ID]!!
                FOLLOW_TYPE -> key = remoteMessage.data[UID]!!
            }

            key?.let {
                map[TYPE] = type
                map[KEY] = key
            }

            val data = Data.Builder()
            data.putAll(map)

            WorkManager.getInstance(applicationContext).enqueue(
                OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                    .setInputData(data.build()).build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "onMessageReceived: ", e)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "onTaskRemoved: fcm service task removed")
        val restartServiceIntent = Intent(applicationContext, ZingFirebaseMessagingService::class.java)
        restartServiceIntent.setPackage(packageName)
        val restartServicePendingIntent = PendingIntent
            .getService(applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT)
        val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000] =
            restartServicePendingIntent
        Log.d(TAG, "onTaskRemoved: fcm service completed")
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        private const val TAG = "ZingFirebaseMessagingSe"
    }
}