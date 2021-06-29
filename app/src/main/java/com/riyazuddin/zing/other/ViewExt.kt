package com.riyazuddin.zing.other

import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.riyazuddin.zing.R

fun View.slideUp(context: Context, animTime: Long, startOffset: Long) {
    val slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up).apply {
        duration = animTime
        interpolator = FastOutSlowInInterpolator()
        this.startOffset = startOffset
    }
    startAnimation(slideUp)
}

fun slideUpViews(context: Context, vararg views: View, animTime: Long = 300L, delay: Long = 150) {
    for (i in views.indices) {
        views[i].slideUp(context, animTime, delay * i)
    }
}