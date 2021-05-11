package com.riyazuddin.zing.services

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.NavHostController
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Constants.CHANNEL_ID
import com.riyazuddin.zing.other.Constants.USERS_COLLECTION
import com.riyazuddin.zing.ui.main.fragments.HomeFragment
import com.riyazuddin.zing.ui.main.fragments.chat.ChatFragmentArgs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.i("FMS", "onNewToken: $token")

        Firebase.auth.currentUser?.let {
            GlobalScope.launch {
                Log.i("FMS", "onNewToken: updating")
                val map = mapOf(
                    "token" to token
                )
                FirebaseFirestore.getInstance().collection(USERS_COLLECTION)
                    .document(it.uid)
                    .set(map, SetOptions.merge()).await()
                Log.i("FMS", "onNewToken: updated")
            }
        }

    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i("FCMLog", "onMessageReceived: ${remoteMessage.notification}")

        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        val v = remoteMessage.data["currentUser"]
        Log.i("FCMLog", "onMessageReceived: $v")
        val gson = Gson()
        val currentUser = gson.fromJson(remoteMessage.data["currentUser"], User::class.java)
        val otherEndUser = gson.fromJson(remoteMessage.data["otherEndUser"], User::class.java)

        Log.i("FCMLog", "onMessageReceived: c ---> $currentUser")
        Log.i("FCMLog", "onMessageReceived: o ---> $otherEndUser")

        val b = Bundle().apply {

        }

        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setGraph(R.navigation.nav_graph_main)
            .setDestination(R.id.chatFragment)
            .setArguments(ChatFragmentArgs(otherEndUser, currentUser).toBundle())
            .createPendingIntent()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(1,builder.build())

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}