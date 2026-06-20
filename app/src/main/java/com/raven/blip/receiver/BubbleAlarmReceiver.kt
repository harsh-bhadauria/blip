package com.raven.blip.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raven.blip.ui.overlay.OverlayService

class BubbleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_BUBBLE
        }
        context.startService(serviceIntent)
        
        // Note: The rescheduling logic will be triggered either by the service 
        // after showing the bubble, or we could do it here. 
        // We'll let the ViewModel or Service handle rescheduling so it uses 
        // the correct user settings for intervals.
    }
}
