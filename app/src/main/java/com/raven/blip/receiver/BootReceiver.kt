package com.raven.blip.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raven.blip.ui.overlay.OverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            // On Android 8.0+, we must use startForegroundService
            context.startForegroundService(serviceIntent)
        }
    }
}
