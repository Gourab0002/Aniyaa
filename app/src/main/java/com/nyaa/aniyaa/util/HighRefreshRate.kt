package com.nyaa.aniyaa.util

import android.app.Activity
import android.os.Build
import android.view.Display
import android.view.View

/**
 * Locks the activity window to the display's highest refresh-rate mode.
 *
 * Many 120 Hz+ phones ignore [android.view.WindowManager.LayoutParams.preferredRefreshRate]
 * and only switch modes when [android.view.WindowManager.LayoutParams.preferredDisplayModeId]
 * is set. Android 15 also lets the view tree request a high frame-rate category.
 */
object HighRefreshRate {

    fun apply(activity: Activity) {
        val display = currentDisplay(activity) ?: return
        val mode = bestMode(display) ?: return

        val params = activity.window.attributes
        params.preferredRefreshRate = mode.refreshRate
        params.preferredDisplayModeId = mode.modeId
        activity.window.attributes = params

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setPreferMinimalPostProcessing(true)
        }

        val decor = activity.window.decorView
        requestHighFrameRate(decor)
        if (!decor.isAttachedToWindow) {
            decor.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    requestHighFrameRate(v)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })
        }
    }

    private fun currentDisplay(activity: Activity): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }
    }

    private fun bestMode(display: Display): Display.Mode? {
        val modes = display.supportedModes
        if (modes.isEmpty()) return null
        val current = display.mode
        val sameResolution = modes.filter { mode ->
            mode.physicalWidth == current.physicalWidth &&
                mode.physicalHeight == current.physicalHeight
        }
        val candidates = if (sameResolution.isNotEmpty()) sameResolution else modes.toList()
        return candidates.maxByOrNull { it.refreshRate }
    }

    private fun requestHighFrameRate(view: View) {
        if (Build.VERSION.SDK_INT >= 35) {
            view.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH)
        }
    }
}
