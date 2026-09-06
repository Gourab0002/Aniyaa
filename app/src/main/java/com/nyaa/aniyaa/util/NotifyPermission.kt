package com.nyaa.aniyaa.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.nyaa.aniyaa.work.SavedSearchWorker

fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

fun prepareSavedSearchAlerts(context: Context) {
    SavedSearchWorker.ensureChannel(context)
    SavedSearchWorker.enqueue(context)
}
