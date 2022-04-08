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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.riyazuddin.zing.other.Constants.CHAT_CHANNEL_ID
import com.riyazuddin.zing.other.Constants.NORMAL_NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.HiltAndroidApp

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
        val soundUri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHAT_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHAT_CHANNEL_ID,
                    getString(R.string.chat_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        getString(R.string.chat_channel_description)
                    setSound(soundUri, audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }
            if (notificationManager.getNotificationChannel(NORMAL_NOTIFICATION_CHANNEL_ID) == null) {
                val channel =
                    NotificationChannel(
                        NORMAL_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.normal_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description =
                            getString(R.string.normal_channel_description)
                        setSound(soundUri, audioAttributes)
                    }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

}