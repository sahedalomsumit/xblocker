package com.sahed.xblocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sahed.xblocker.data.datastore.AppPreferences
import javax.inject.Inject

/**
 * Boot receiver that syncs the blocker-active preference with the real
 * accessibility service state after a device restart.
 *
 * Note: Accessibility Services are automatically restarted by Android after boot
 * if the user had them enabled — no manual restart is required.
 * We only need to sync the stored preference in case it was left stale.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        CoroutineScope(Dispatchers.IO).launch {
            // Sync stored state with actual accessibility service state
            val serviceRunning = XBlockerAccessibilityService.isRunning(context)
            preferences.setBlockerActive(serviceRunning)
            Log.i("BootReceiver", "Boot sync — accessibility service running: $serviceRunning")
        }
    }
}
