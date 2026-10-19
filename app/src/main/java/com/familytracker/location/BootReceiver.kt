package com.familytracker.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts tracking after a reboot or an app update. Both broadcasts may start a location FGS. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> LocationService.start(context)
        }
    }
}
