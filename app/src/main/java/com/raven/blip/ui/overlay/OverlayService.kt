package com.raven.blip.ui.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.raven.blip.domain.model.OverlayCorner
import com.raven.blip.domain.model.OverlayState
import com.raven.blip.domain.repository.SettingsRepository
import com.raven.blip.domain.repository.SkinRepository
import com.raven.blip.domain.repository.TaskRepository
import com.raven.blip.ui.overlay.components.blob.BlobOverlay
import com.raven.blip.ui.overlay.components.bubble.BubbleOverlay
import com.raven.blip.ui.overlay.components.panel.PanelOverlay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    companion object {
        var isRunning = false
        const val ACTION_SHOW_BUBBLE = "com.raven.blip.ACTION_SHOW_BUBBLE"
        private const val CHANNEL_ID = "blip_overlay_channel"
        private const val NOTIFICATION_ID = 1
        private const val BLOB_TOTAL_SIZE_DP = 52
        private const val BLOB_MARGIN_PX = 16
        private const val PANEL_GAP_DP = 4
    }

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var skinRepository: SkinRepository

    private var blobView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var isPanelAttached = false
    private var bubbleView: ComposeView? = null
    private var isBubbleAttached = false
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var viewModel: OverlayViewModel? = null
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundWithNotification()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_BUBBLE) {
            viewModel?.showNextBubble()
            scheduleNextBubble(this)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeOverlay()
    }

    // --- Notification ---

    private fun startForegroundWithNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Blip Overlay",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Keeps Blip running as an overlay"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Blip is active")
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    // --- Overlay ---

    private fun showOverlay() {
        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner
        owner.onCreate()

        val density = resources.displayMetrics.density
        val blobTotalPx = (BLOB_TOTAL_SIZE_DP * density).toInt()
        val panelGapPx = (PANEL_GAP_DP * density).toInt()

        val factory = OverlayViewModelFactory(taskRepository, settingsRepository, skinRepository)
        viewModel = ViewModelProvider(owner, factory)[OverlayViewModel::class.java]

        // Schedule first bubble right away using the configured interval
        scheduleNextBubble(this)

        // Panel starts NOT_FOCUSABLE; we remove that flag only when keyboard is needed
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = BLOB_MARGIN_PX
            y = BLOB_MARGIN_PX + blobTotalPx + panelGapPx
            windowAnimations = 0
        }

        // --- Panel view ---
        val panel = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setOnTouchListener { view, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    viewModel?.collapse()
                    view.performClick()
                    true
                } else {
                    false
                }
            }
            setContent {
                MaterialTheme {
                    PanelOverlay(
                        viewModel = viewModel!!,
                        onDismissed = ::detachPanel
                    )
                }
            }
        }
        panelView = panel

        // --- Bubble view ---
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = BLOB_MARGIN_PX
            y = BLOB_MARGIN_PX + blobTotalPx + panelGapPx
            windowAnimations = 0
        }

        val bubble = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                MaterialTheme {
                    BubbleOverlay(
                        viewModel = viewModel!!,
                        onDismissed = ::detachBubble
                    )
                }
            }
        }
        bubbleView = bubble

        // --- Blob view ---
        val blob = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                MaterialTheme {
                    LaunchedEffect(viewModel!!.overlayState) {
                        when {
                            viewModel!!.isPanelVisible -> attachPanel()
                            viewModel!!.overlayState == OverlayState.ADDING_TASK -> {
                                attachPanel()
                                enableKeyboardFocus()
                            }
                        }
                        if (viewModel!!.overlayState == OverlayState.BUBBLE) {
                            attachBubble()
                        }
                        // Keyboard focus: enable when adding, disable otherwise
                        if (viewModel!!.isAddingTask) {
                            enableKeyboardFocus()
                        } else {
                            disableKeyboardFocus()
                        }
                    }
                    BlobOverlay(viewModel = viewModel!!)
                }
            }
        }
        blobView = blob

        val blobParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = BLOB_MARGIN_PX
            y = BLOB_MARGIN_PX
            windowAnimations = 0
        }
        windowManager.addView(blob, blobParams)

        lifecycleOwner?.lifecycleScope?.launch {
            viewModel!!.settingsFlow.collect { settings ->
                val newGravity = when (settings.corner) {
                    OverlayCorner.TOP_START -> Gravity.TOP or Gravity.START
                    OverlayCorner.TOP_END -> Gravity.TOP or Gravity.END
                    OverlayCorner.BOTTOM_START -> Gravity.BOTTOM or Gravity.START
                    OverlayCorner.BOTTOM_END -> Gravity.BOTTOM or Gravity.END
                }
                
                if (blobParams.gravity != newGravity) {
                    blobParams.gravity = newGravity
                    windowManager.updateViewLayout(blobView, blobParams)

                    if (isPanelAttached) {
                        panelParams.gravity = newGravity
                        windowManager.updateViewLayout(panelView, panelParams)
                    }
                    if (isBubbleAttached) {
                        bubbleParams.gravity = newGravity
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                    }
                }
            }
        }


    }

    private fun attachPanel() {
        if (isPanelAttached) return
        panelView?.let {
            windowManager.addView(it, panelParams)
            isPanelAttached = true
        }
    }

    private fun detachPanel() {
        if (!isPanelAttached) return
        disableKeyboardFocus()
        panelView?.let {
            windowManager.removeView(it)
            isPanelAttached = false
        }
    }

    private fun attachBubble() {
        if (isBubbleAttached) return
        bubbleView?.let {
            windowManager.addView(it, bubbleParams)
            isBubbleAttached = true
        }
    }

    private fun detachBubble() {
        if (!isBubbleAttached) return
        bubbleView?.let {
            windowManager.removeView(it)
            isBubbleAttached = false
        }
    }

    /**
     * Remove FLAG_NOT_FOCUSABLE so the overlay can receive keyboard input.
     * Must be called before the text field requests focus.
     */
    private fun enableKeyboardFocus() {
        if (!isPanelAttached) return
        panelParams.flags = panelParams.flags and
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        windowManager.updateViewLayout(panelView, panelParams)
    }

    /**
     * Restore FLAG_NOT_FOCUSABLE so touches pass through to apps below
     * when we're back in list mode or collapsed.
     */
    private fun disableKeyboardFocus() {
        panelParams.flags = panelParams.flags or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (isPanelAttached) {
            windowManager.updateViewLayout(panelView, panelParams)
        }
    }

    private fun removeOverlay() {
        detachPanel()
        detachBubble()
        blobView?.let {
            windowManager.removeView(it)
            blobView = null
        }
        panelView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        viewModel = null
    }



    private fun getNextBubbleDelayMs(settings: com.raven.blip.domain.model.AppSettings?): Long {
        val intervalMs = settings?.bubbleIntervalMs ?: 15 * 60 * 1000L
        if (settings == null) return intervalMs

        val startHour = settings.quietHoursStart
        val endHour = settings.quietHoursEnd
        
        if (startHour == null || endHour == null) return intervalMs
        
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        val isQuietHours = if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour >= startHour || currentHour < endHour
        }
        
        if (isQuietHours) {
            val targetCalendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, endHour)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (currentHour >= endHour) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            // Add a 1 minute buffer to ensure we wake up *after* quiet hours end
            return targetCalendar.timeInMillis - System.currentTimeMillis() + 60_000L
        }
        
        return intervalMs
    }

    private fun scheduleNextBubble(context: android.content.Context) {
        val intervalMs = getNextBubbleDelayMs(viewModel?.settingsFlow?.value)
        val am = context.getSystemService(android.app.AlarmManager::class.java)
        val pi = android.app.PendingIntent.getBroadcast(
            context, 0,
            Intent(context, com.raven.blip.receiver.BubbleAlarmReceiver::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + intervalMs,
                pi
            )
            return
        }

        am.setExactAndAllowWhileIdle(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + intervalMs,
            pi
        )
    }
}
