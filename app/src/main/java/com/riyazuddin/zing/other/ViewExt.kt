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
        views[i].slideUp(context, animTime, delay*i)
    }
}

fun TextView.leftDrawable(@DrawableRes id: Int = 0, @DimenRes sizeRes: Int = 0, @ColorInt color: Int = 0, @ColorRes colorRes: Int = 0) {
    val drawable = ContextCompat.getDrawable(context, id)
    if (sizeRes != 0) {
        val size = resources.getDimension(sizeRes)
        drawable?.setBounds(0, 0, size.toInt(), size.toInt())
    }
    if (color != 0) {
        drawable?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    } else if (colorRes != 0) {
        val colorInt = ContextCompat.getColor(context, colorRes)
        drawable?.setColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP)
    }
    this.setCompoundDrawables(drawable, null, null, null)
}