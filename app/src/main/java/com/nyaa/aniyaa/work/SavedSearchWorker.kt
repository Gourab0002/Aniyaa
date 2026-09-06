package com.nyaa.aniyaa.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nyaa.aniyaa.MainActivity
import com.nyaa.aniyaa.R
import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.repository.NyaaRepository
import com.nyaa.aniyaa.data.repository.SavedSearchRepository
import java.util.concurrent.TimeUnit

class SavedSearchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.get(applicationContext)
        val savedRepo = SavedSearchRepository(database)
        val repository = NyaaRepository()
        val notifying = savedRepo.getNotifying()
        if (notifying.isEmpty()) return Result.success()
        ensureChannel(applicationContext)

        notifying.forEach { saved ->
            val result = repository.search(saved.toSearchParams(), forceNetwork = true)
            val torrents = result.getOrNull().orEmpty()
            if (torrents.isEmpty()) return@forEach
            val ids = torrents.map { it.id }.filter { it.isNotBlank() }
            val previous = saved.lastSeenIds.split(',').filter { it.isNotBlank() }.toSet()
            val newIds = if (previous.isEmpty()) emptyList() else ids.filterNot { it in previous }
            savedRepo.update(
                saved.copy(
                    lastSeenIds = ids.take(40).joinToString(","),
                    lastCheckedAt = System.currentTimeMillis()
                )
            )
            if (newIds.isNotEmpty()) {
                notifyNewResults(applicationContext, saved.id, saved.displayName(), newIds.size)
            }
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "saved-search-alerts"
        private const val CHANNEL_ID = "saved_searches"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SavedSearchWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Saved search alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
        }

        private fun notifyNewResults(context: Context, savedId: Long, name: String, count: Int) {
            val intent = Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("aniyaa://saved/$savedId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                savedId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New results for $name")
                .setContentText("$count new listing${if (count == 1) "" else "s"}")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(1000 + savedId.toInt(), notification)
            } catch (_: SecurityException) {
            }
        }
    }
}
