package com.riyazuddin.zing

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.riyazuddin.zing.other.Constants.CHATTING_WITH
import com.riyazuddin.zing.other.Constants.CHAT_CHANNEL_ID
import com.riyazuddin.zing.other.Constants.NORMAL_NOTIFICATION_CHANNEL_ID
import com.riyazuddin.zing.other.Constants.NO_ONE
import com.riyazuddin.zing.other.Constants.UID
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        setSharedPreference()
        createNotificationChannel()
    }

    private fun setSharedPreference() {
        val sp = getSharedPreferences(CHATTING_WITH, MODE_PRIVATE)
        sp.edit().let {
            it.putString(UID, NO_ONE)
            it.apply()
        }

        val sp1 = getSharedPreferences("isFirstLoadOfRecentChat", MODE_PRIVATE)
        sp1.edit().let {
            it.putBoolean("isFirstLoadOfRecentChat", true)
            it.apply()
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat"
            val descriptionText = "This channel used for chat messages, it cam make sound and show pop up notification"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHAT_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Normal"
            val descriptionText = "This channel used for Normal notification like New Follower, Comment, Like etc..."
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(NORMAL_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}