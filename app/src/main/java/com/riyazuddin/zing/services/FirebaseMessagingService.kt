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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.Notification
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.CHATS_COLLECTION
import com.riyazuddin.zing.other.Constants.CHATTING_WITH
import com.riyazuddin.zing.other.Constants.CHAT_THREAD
import com.riyazuddin.zing.other.Constants.CHAT_TYPE
import com.riyazuddin.zing.other.Constants.COMMENT_TYPE
import com.riyazuddin.zing.other.Constants.DELIVERED
import com.riyazuddin.zing.other.Constants.FOLLOW_TYPE
import com.riyazuddin.zing.other.Constants.MESSAGES_COLLECTION
import com.riyazuddin.zing.other.Constants.MESSAGE_ID
import com.riyazuddin.zing.other.Constants.NOTIFICATION_ID
import com.riyazuddin.zing.other.Constants.NO_ONE
import com.riyazuddin.zing.other.Constants.POST_ID
import com.riyazuddin.zing.other.Constants.POST_LIKE_TYPE
import com.riyazuddin.zing.other.Constants.SEEN
import com.riyazuddin.zing.other.Constants.STATUS
import com.riyazuddin.zing.other.Constants.TOKEN
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.ui.main.fragments.OthersProfileFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.PostFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.chat.ChatFragmentArgs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseMessaging.getInstance().subscribeToTopic("PUSH_RC")
        Log.i(TAG, "onNewToken: $token")
        Firebase.auth.currentUser?.let {
            GlobalScope.launch {
                val map = mapOf(
                    TOKEN to token
                )
                FirebaseFirestore.getInstance().collection(USERS_COLLECTION)
                    .document(it.uid)
                    .set(map, SetOptions.merge()).await()
                Log.i(TAG, "onNewToken: updated")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.i(TAG, "onMessageReceived: ")
        val notificationDataObj =
            Gson().fromJson(remoteMessage.data["notification"], Notification::class.java)

        val sp = getSharedPreferences(CHATTING_WITH, MODE_PRIVATE)
        val uid = sp.getString(UID, NO_ONE)
        if (notificationDataObj.tag == uid) {
            Log.i(TAG, "onMessageReceived: returning")
            return
        }

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        notificationLayout.setTextViewText(R.id.title, notificationDataObj.title)
        notificationLayout.setTextViewText(R.id.body, notificationDataObj.body)

        val bitmap =
            Glide.with(this).asBitmap().load(notificationDataObj.image).circleCrop().submit().get()
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

        when (remoteMessage.data["type"]) {
            CHAT_TYPE -> {
                GlobalScope.launch {
                    val chatThread = remoteMessage.data[CHAT_THREAD]!!
                    val messageId = remoteMessage.data[MESSAGE_ID]!!
                    val threadRef = FirebaseFirestore.getInstance().collection(CHATS_COLLECTION)
                        .document(chatThread)
                    val message = threadRef.collection(MESSAGES_COLLECTION).document(messageId)
                        .get().await().toObject(Message::class.java)!!
                    if (message.status != SEEN) {
                        threadRef.collection(MESSAGES_COLLECTION)
                            .document(messageId)
                            .update(STATUS, DELIVERED).await()
                        val pi = NavDeepLinkBuilder(baseContext)
                            .setGraph(R.navigation.nav_graph_main)
                            .setDestination(R.id.chatFragment)
                            .setArguments(
                                ChatFragmentArgs(
                                    Gson().fromJson(remoteMessage.data["ou"], User::class.java),
                                    User()
                                ).toBundle()
                            )
                            .createPendingIntent()
                        notification.setContentIntent(pi)
                        notifyNotification(notificationDataObj.tag, notification.build())
                    }
                }
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