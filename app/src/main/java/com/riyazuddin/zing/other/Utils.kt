package com.riyazuddin.zing.other

import android.content.Context
import androidx.core.content.ContextCompat.getColor
import com.riyazuddin.zing.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

class Utils @Inject constructor(@ApplicationContext val context: Context) {

    companion object {
        private const val SECOND_MILLIS = 1000
        private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
        private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private const val DAY_MILLIS = 24 * HOUR_MILLIS

        fun getTimeAgo(time: Long): String? {
            val now: Long = System.currentTimeMillis()
            if (time > now || time <= 0) {
                return null
            }

            val diff = now - time
            return if (diff < MINUTE_MILLIS) {
                "just now"
            } else if (diff < 2 * MINUTE_MILLIS) {
                "a minute ago"
            } else if (diff < 50 * MINUTE_MILLIS) {
                (diff / MINUTE_MILLIS).toString() + " minutes ago"
            } else if (diff < 90 * MINUTE_MILLIS) {
                "an hour ago"
            } else if (diff < 24 * HOUR_MILLIS) {
                (diff / HOUR_MILLIS).toString() + " hours ago"
            } else if (diff < 48 * HOUR_MILLIS) {
                "yesterday"
            } else if (diff < 168 * HOUR_MILLIS) {
                (diff / DAY_MILLIS).toString() + " days ago"
            } else {
                val s = SimpleDateFormat("dd MMM yyyy", Locale.US)
                s.format(time)
            }
        }
    }

    fun randomPrideColor(): Int {
        val arr = listOf(
            getColor(context, R.color.pride1),
            getColor(context, R.color.pride2),
            getColor(context, R.color.pride3),
            getColor(context, R.color.pride4),
            getColor(context, R.color.pride5),
            getColor(context, R.color.pride6),
            getColor(context, R.color.pride7),
            getColor(context, R.color.pride8)
        )
        val random = Random.nextInt(0, 8)
        return arr[random]
    }
}