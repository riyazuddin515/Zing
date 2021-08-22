package com.riyazuddin.zing.workers

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.Notification
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.CHAT_TYPE
import com.riyazuddin.zing.other.Constants.COMMENT_TYPE
import com.riyazuddin.zing.other.Constants.FOLLOW_TYPE
import com.riyazuddin.zing.other.Constants.KEY
import com.riyazuddin.zing.other.Constants.POST_LIKE_TYPE
import com.riyazuddin.zing.other.Constants.TYPE
import com.riyazuddin.zing.ui.main.fragments.OthersProfileFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.PostFragmentArgs
import com.riyazuddin.zing.ui.main.fragments.stream_chat.ChatFragmentArgs
import toDataClass

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
            if (type == CHAT_TYPE) {
                notificationDataObj.image = notificationDataObj.image.replace("\\", "")
            }

            val notificationLayout =
                RemoteViews(applicationContext.packageName, R.layout.notification_layout)
            notificationLayout.setTextViewText(
                R.id.title,
                if (notificationDataObj.title.isNotEmpty()) notificationDataObj.title else "Media File 📁"
            )
            notificationLayout.setTextViewText(R.id.body, notificationDataObj.body)

            notificationLayout.setImageViewBitmap(
                R.id.imageView,
                getProfileBitmap(notificationDataObj.image)
            )

            val notification =
                NotificationCompat.Builder(applicationContext, notificationDataObj.channel_id)
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
                    val pi = NavDeepLinkBuilder(applicationContext)
                        .setGraph(R.navigation.nav_graph_main)
                        .setDestination(R.id.streamChatFragment)
                        .setArguments(ChatFragmentArgs(key).toBundle())
                        .createPendingIntent()
                    notification.setContentIntent(pi)
                }
                FOLLOW_TYPE -> {
                    val pi = NavDeepLinkBuilder(applicationContext)
                        .setGraph(R.navigation.nav_graph_main)
                        .setDestination(R.id.othersProfileFragment)
                        .setArguments(OthersProfileFragmentArgs(key).toBundle())
                        .createPendingIntent()
                    notification.setContentIntent(pi)
                }
                POST_LIKE_TYPE, COMMENT_TYPE -> {
                    val pi = NavDeepLinkBuilder(applicationContext)
                        .setGraph(R.navigation.nav_graph_main)
                        .setDestination(R.id.postFragment)
                        .setArguments(PostFragmentArgs(key).toBundle())
                        .createPendingIntent()
                    notification.setContentIntent(pi)
                }
            }
            notifyNotification(notificationDataObj.tag, notification.build())
            Result.Success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork: ", e)
            Result.failure()
        }
    }

    private fun notifyNotification(tag: String, notification: android.app.Notification) {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(tag, Constants.NOTIFICATION_ID, notification)
    }

    private fun getProfileBitmap(url: String): Bitmap {
        return try {
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