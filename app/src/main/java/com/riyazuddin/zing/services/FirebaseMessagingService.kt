package com.riyazuddin.zing.services

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.Notification
import com.riyazuddin.zing.other.Constants.CHAT_TYPE
import com.riyazuddin.zing.other.Constants.COMMENT_TYPE
import com.riyazuddin.zing.other.Constants.FOLLOW_TYPE
import com.riyazuddin.zing.other.Constants.NOTIFICATION_ID
import com.riyazuddin.zing.other.Constants.POST_ID
import com.riyazuddin.zing.other.Constants.POST_LIKE_TYPE
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.ui.main.fragments.OthersProfileFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.PostFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.stream_chat.ChatFragmentArgs
import io.getstream.chat.android.client.ChatClient


class FirebaseMessagingService : FirebaseMessagingService() {

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
            Log.i(TAG, "onMessageReceived: ${remoteMessage.data}")

            val notificationDataObj =
                Gson().fromJson(remoteMessage.data["notification"], Notification::class.java)

            val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
            notificationLayout.setTextViewText(R.id.title, notificationDataObj.title)
            notificationLayout.setTextViewText(R.id.body, notificationDataObj.body)

            val type = remoteMessage.data["type"]!!
            if (type == CHAT_TYPE) {
                notificationDataObj.image = notificationDataObj.image.replace("\\", "")
            }

            val bitmap = Glide.with(this)
                .asBitmap()
                .load(notificationDataObj.image)
                .circleCrop().submit().get()
            notificationLayout.setImageViewBitmap(R.id.imageView, bitmap)

            val notification = NotificationCompat.Builder(this, notificationDataObj.channel_id)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification))
                .setAutoCancel(true)

            when (type) {
                CHAT_TYPE -> {
                    val pi = NavDeepLinkBuilder(this)
                        .setGraph(R.navigation.nav_graph_main)
                        .setDestination(R.id.streamChatFragment)
                        .setArguments(ChatFragmentArgs(channelId = remoteMessage.data["cid"]!!).toBundle())
                        .createPendingIntent()
                    notification.setContentIntent(pi)
                    notifyNotification(notificationDataObj.tag, notification.build())
                }
                FOLLOW_TYPE -> {
                    val pi = NavDeepLinkBuilder(this)
                        .setGraph(R.navigation.nav_graph_main)
                        .setDestination(R.id.othersProfileFragment)
                        .setArguments(OthersProfileFragmentArgs(remoteMessage.data[UID]!!).toBundle())
                        .createPendingIntent()
                    notification.setContentIntent(pi)
                    notifyNotification(notificationDataObj.tag, notification.build())
                }
                POST_LIKE_TYPE, COMMENT_TYPE -> {
                    val pi = NavDeepLinkBuilder(this)
                        .setGraph(R.navigation.nav_graph_main)
                        .setDestination(R.id.postFragment)
                        .setArguments(PostFragmentArgs(remoteMessage.data[POST_ID]!!).toBundle())
                        .createPendingIntent()
                    notification.setContentIntent(pi)
                    notifyNotification(notificationDataObj.tag, notification.build())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onMessageReceived: ", e)
        }
    }

    private fun notifyNotification(tag: String, notification: android.app.Notification) {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(tag, NOTIFICATION_ID, notification)
    }

    companion object {
        const val TAG = "FCS"
    }
}