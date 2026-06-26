package com.sahed.xblocker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sahed.xblocker.data.datastore.AppPreferences
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.data.repository.TimerRepository
import com.sahed.xblocker.domain.model.TimerEventType
import com.sahed.xblocker.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TimerWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val timerRepo: TimerRepository,
    private val blocklistRepo: BlocklistRepository,
    private val preferences: AppPreferences
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_EVENT_TYPE = "event_type"
        const val KEY_DOMAIN_ID = "domain_id"
        const val KEY_TIMER_ID = "timer_id"
        const val TAG_DISABLE = "disable_blocker"
        const val TAG_REMOVE_DOMAIN = "remove_domain"
        const val WORK_DISABLE_BLOCKER = "xblocker_disable"
        const val WORK_REMOVE_DOMAIN = "xblocker_remove_domain"
        private const val CHANNEL_ID = "xblocker_timer_channel"
        private const val TAG = "TimerWorker"
    }

    override suspend fun doWork(): Result {
        val eventType = inputData.getString(KEY_EVENT_TYPE) ?: return Result.failure()

        return try {
            when (TimerEventType.valueOf(eventType)) {
                TimerEventType.DISABLE_BLOCKER -> handleDisableBlocker()
                TimerEventType.REMOVE_DOMAIN -> handleRemoveDomain()
            }
            sendCompletionNotification(eventType)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Timer worker failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun handleDisableBlocker() {
        // Stop the VPN service
        val stopIntent = Intent(appContext, XBlockerVpnService::class.java).apply {
            action = XBlockerVpnService.ACTION_STOP
        }
        appContext.startService(stopIntent)
        preferences.setVpnActive(false)

        // Mark all disable timers as complete
        val pending = timerRepo.getPendingEventsOnce()
        pending.filter { it.eventType == TimerEventType.DISABLE_BLOCKER }.forEach {
            timerRepo.markComplete(it.id)
        }
        Log.i(TAG, "Blocker disabled after 3-hour timer")
    }

    private suspend fun handleRemoveDomain() {
        val domainId = inputData.getLong(KEY_DOMAIN_ID, -1L)
        if (domainId == -1L) return

        blocklistRepo.deleteById(domainId)

        // Mark timer complete
        val pending = timerRepo.getPendingEventsOnce()
        pending.filter { it.eventType == TimerEventType.REMOVE_DOMAIN && it.domainId == domainId }
            .forEach { timerRepo.markComplete(it.id) }

        Log.i(TAG, "Domain $domainId deleted after 3-hour timer")
    }

    private fun sendCompletionNotification(eventType: String) {
        createNotificationChannel()
        val manager = appContext.getSystemService(NotificationManager::class.java)
        val openIntent = PendingIntent.getActivity(
            appContext, 0,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val (title, body) = when (eventType) {
            "DISABLE_BLOCKER" -> "Blocker Disabled" to "XBlocker has been turned off."
            "REMOVE_DOMAIN" -> "Domain Removed" to "The domain has been removed from your blocklist."
            else -> "Timer Complete" to "Your XBlocker timer has finished."
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "XBlocker Timers",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Timer completion alerts" }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
