package com.riyazuddin.zing.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.Notification
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.KEY
import com.riyazuddin.zing.other.Constants.TYPE
import com.riyazuddin.zing.services.DirectReplyReceiver
import com.riyazuddin.zing.ui.main.fragments.OthersProfileFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.PostFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.stream_chat.ChatFragmentArgs
import kotlinx.coroutines.runBlocking
import toDataClass
import kotlin.random.Random

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) :
    Worker(context, params) {

    companion object {
        const val TAG = "NotificationWorker"
    }

    override fun doWork(): Result {
        return try {

            val map = inputData.keyValueMap
            val notificationDataObj = map.toDataClass<Notification>()

            val type = map[TYPE]!!.toString()
            val key = map[KEY]!!.toString()

            val notification = createNotification(notificationDataObj, type, key)
            notifyNotification(notificationDataObj.tag, notification)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork: ", e)
            Result.failure()
        }
    }


    private fun createNotification(notificationDataObj: Notification, type: String, key: String, ): android.app.Notification {
        val notificationLayout =
            RemoteViews(applicationContext.packageName, R.layout.notification_layout)
        notificationLayout.setTextViewText(
            R.id.title,
            notificationDataObj.title.ifEmpty { "Media File 📁" }
        )
        notificationLayout.setTextViewText(R.id.body, notificationDataObj.body)
        notificationLayout.setImageViewBitmap(R.id.imageView, getProfileBitmap(notificationDataObj.image))

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                notificationDataObj.notification_channel_id
            )
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification))
                .setAutoCancel(true)

        when (type) {
            Constants.CHAT_TYPE -> {

                val b = Bundle().apply {
                    putSerializable("notificationObj", notificationDataObj)
                }

                val i = Intent(applicationContext, DirectReplyReceiver::class.java)
                i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                i.putExtras(b)

                val rand = Random.nextInt(0, 999999)
                val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getBroadcast(
                        applicationContext, rand, i, PendingIntent.FLAG_MUTABLE
                    )
                } else {
                    PendingIntent.getBroadcast(
                        applicationContext, rand, i, PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
                val ri = RemoteInput.Builder(Constants.DIRECT_REPLY)
                    .setLabel(applicationContext.resources.getString(R.string.reply)).build()
                val action = NotificationCompat.Action.Builder(
                    R.drawable.ic_notification_icon,
                    applicationContext.resources.getString(R.string.reply),
                    pi
                ).addRemoteInput(ri).build()


                val cpi = NavDeepLinkBuilder(applicationContext)
                    .setGraph(R.navigation.nav_graph_main)
                    .setDestination(R.id.streamChatFragment)
                    .setArguments(ChatFragmentArgs.Builder(key).build().toBundle())
                    .createPendingIntent()

                notification
                    .addAction(action)
                    .setContentIntent(cpi)
            }
            Constants.FOLLOW_TYPE -> {
                val pi = NavDeepLinkBuilder(applicationContext)
                    .setGraph(R.navigation.nav_graph_main)
                    .setDestination(R.id.othersProfileFragment)
                    .setArguments(OthersProfileFragmentArgs.Builder(key).build().toBundle())
                    .createPendingIntent()
                notification.setContentIntent(pi)
            }
            Constants.POST_LIKE_TYPE, Constants.COMMENT_TYPE -> {
                val pi = NavDeepLinkBuilder(applicationContext)
                    .setGraph(R.navigation.nav_graph_main)
                    .setDestination(R.id.postFragment)
                    .setArguments(PostFragmentArgs.Builder(key).build().toBundle())
                    .createPendingIntent()
                notification.setContentIntent(pi)
            }
        }
        return notification.build()
    }

    private fun notifyNotification(tag: String, notification: android.app.Notification) {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(tag, Constants.NOTIFICATION_ID, notification)
    }

    private fun getProfileBitmap(url: String): Bitmap = runBlocking {
        try {
            Glide.with(applicationContext)
                .asBitmap()
                .load(url)
                .circleCrop().submit().get()
        } catch (e: Exception) {
            Glide.with(applicationContext)
                .asBitmap()
                .load(R.drawable.default_profile_pic)
                .circleCrop().submit().get()
        }
    }
}