package com.riyazuddin.zing

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.riyazuddin.zing.other.Constants.CHAT_CHANNEL_ID
import com.riyazuddin.zing.other.Constants.NORMAL_NOTIFICATION_CHANNEL_ID
import com.riyazuddin.zing.workers.ServiceChecker
import dagger.hilt.android.HiltAndroidApp
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.livedata.ChatDomain
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ZingApplication : Application() {

    companion object {
        val TAG: String = ZingApplication::class.java.name
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        initFirebaseRemoteConfig()




        Firebase.auth.uid?.let {
            FirebaseCrashlytics.getInstance().setUserId(it)
        } ?: FirebaseCrashlytics.getInstance().setUserId("")

//        val workRequest = PeriodicWorkRequestBuilder<ServiceChecker>(15, TimeUnit.MINUTES)
//            .addTag(TAG)
//            .build()
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            TAG,
//            ExistingPeriodicWorkPolicy.KEEP,
//            workRequest
//        )
    }

    private fun initFirebaseRemoteConfig() {
        try {
            FirebaseApp.initializeApp(this)
            FirebaseRemoteConfig.getInstance().apply {
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
                setConfigSettingsAsync(configSettings)
                setDefaultsAsync(R.xml.remote_config_defaults)
                fetchAndActivate().addOnCompleteListener {
                    val update = it.result
                    if (it.isSuccessful) {
                        Log.d(TAG, "initFirebaseRemoteConfig: ${it.result}")
                    } else
                        Log.d(TAG, "initFirebaseRemoteConfig: $update")
                }.addOnFailureListener {
                    throw it
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "initFirebaseRemoteConfig: ", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }

    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val soundUri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat"
            val descriptionText =
                "This channel used for chat messages, it cam make sound and show pop up notification"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHAT_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(soundUri, audioAttributes)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Normal"
            val descriptionText =
                "This channel used for Normal notification like New Follower, Comment, Like etc..."
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel =
                NotificationChannel(NORMAL_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    setSound(soundUri, audioAttributes)
                }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}