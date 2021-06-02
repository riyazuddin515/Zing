package com.riyazuddin.zing.services

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
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
import com.riyazuddin.zing.other.Constants.CHANNEL_ID
import com.riyazuddin.zing.other.Constants.CHATTING_WITH
import com.riyazuddin.zing.other.Constants.NOTIFICATION_ID
import com.riyazuddin.zing.other.Constants.NO_ONE
import com.riyazuddin.zing.other.Constants.UID
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
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

        val notification =
            Gson().fromJson(remoteMessage.data["notification"], Notification::class.java)

        val sp = getSharedPreferences(CHATTING_WITH, MODE_PRIVATE)
        val uid = sp.getString(UID, NO_ONE)
        if (notification.tag == uid) {
            Log.i(TAG, "onMessageReceived: returning")
            return
        }

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        notificationLayout.setTextViewText(R.id.title, notification.title)
        notificationLayout.setTextViewText(R.id.body, notification.body)
        val bitmap = Glide.with(this).asBitmap().load(notification.image).submit().get()
        notificationLayout.setImageViewBitmap(R.id.imageView, bitmap)


        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(notification.tag, NOTIFICATION_ID, builder.build())

    }

    companion object {
        const val TAG = "FCS"
    }
}