package com.riyazuddin.zing.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavDeepLinkBuilder
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.Notification
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.CHATTING_WITH
import com.riyazuddin.zing.other.Constants.CHAT_CHANNEL_ID
import com.riyazuddin.zing.other.Constants.NOTIFICATION_ID
import com.riyazuddin.zing.other.Constants.NO_ONE
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.main.fragments.OthersProfileFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.PostFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.chat.ChatFragmentArgs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.i(TAG, "onNewToken: $token")

        Firebase.auth.currentUser?.let {
            GlobalScope.launch {
                Log.i(TAG, "onNewToken: updating")
                val map = mapOf(
                    "token" to token
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

        val notificationDataObj =
            Gson().fromJson(remoteMessage.data["notification"], Notification::class.java)

        val sp = getSharedPreferences(CHATTING_WITH, MODE_PRIVATE)
        val uid = sp.getString(UID, NO_ONE)
        if (notificationDataObj.tag == uid) {
            Log.i(TAG, "onMessageReceived: returning")
            return
        }

        val intent = Intent(this, AuthActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        notificationLayout.setTextViewText(R.id.title, notificationDataObj.title)
        notificationLayout.setTextViewText(R.id.body, notificationDataObj.body)
        val bitmap = Glide.with(this).asBitmap().load(notificationDataObj.image).submit().get()
        notificationLayout.setImageViewBitmap(R.id.imageView, bitmap)

        val notification = NotificationCompat.Builder(this, notificationDataObj.channel_id)
            .setSmallIcon(R.drawable.ic_chat)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification))
            .setAutoCancel(true)

        if (notificationDataObj.channel_id == CHAT_CHANNEL_ID){
            val c = Gson().fromJson(remoteMessage.data["cu"], User::class.java)
            val o = Gson().fromJson(remoteMessage.data["ou"], User::class.java)
            val pi = NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph_main)
                .setDestination(R.id.chatFragment)
                .setArguments(ChatFragmentArgs(o, c).toBundle())
                .createPendingIntent()
            notification.setContentIntent(pi)
        }
        else if(remoteMessage.data["type"] == "FOLLOW"){
            val pi = NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph_main)
                .setDestination(R.id.othersProfileFragment)
                .setArguments(OthersProfileFragmentArgs(remoteMessage.data["uid"]!!).toBundle())
                .createPendingIntent()
            notification.setContentIntent(pi)
        }else if(remoteMessage.data["type"] == "POST_LIKE"){
            val pi = NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph_main)
                .setDestination(R.id.postFragment)
                .setArguments(PostFragmentArgs(remoteMessage.data["post_id"]!!).toBundle())
                .createPendingIntent()
            notification.setContentIntent(pi)
        }

        notifyNotification(notificationDataObj.tag, notification.build())
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