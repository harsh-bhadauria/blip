package com.raven.blip.ui.overlay.components.blob

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.raven.blip.domain.model.BlobEvent
import com.raven.blip.domain.model.BlobVisualState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controls random event triggering for the blob.
 *
 * - Checks every 2-3 minutes (randomised) while IDLE
 * - 10% chance per roll
 * - Hard cooldown of 10 minutes between events
 * - Celebration events are triggered deterministically via [triggerCelebration]
 */
class BlobEventController {

    var activeEvent by mutableStateOf<BlobEvent?>(null)
        private set

    var eventProgress by mutableFloatStateOf(0f)
        private set

    private var currentVisualState: BlobVisualState = BlobVisualState.IDLE
    private var currentUrgency: Float = 0f
    private var lastEventTime: Long = 0L
    private val scope = CoroutineScope(Dispatchers.Main)

    private val hardCooldownMs = 10 * 60 * 1000L  // 10 minutes

    fun updateState(visualState: BlobVisualState, urgency: Float) {
        currentVisualState = visualState
        currentUrgency = urgency
    }

    /** Main event loop — call from a LaunchedEffect(Unit). */
    suspend fun startEventLoop() {
        while (true) {
            // Wait 2-3 minutes (randomised)
            val waitMs = (120_000L + (Math.random() * 60_000L).toLong())
            delay(waitMs)

            tryRollEvent()
        }
    }

    /** Force-trigger the celebrate event (on task completion). */
    fun triggerCelebration() {
        val celebrateEvent = ALL_EVENTS.find { it.id == "celebrate" } ?: return
        if (activeEvent != null) return // don't interrupt an existing event
        launchEvent(celebrateEvent)
    }

    /** Force-trigger an event for testing purposes. If eventId is null, picks randomly. */
    fun triggerRandomEvent(eventId: String? = null) {
        if (activeEvent != null) return // don't interrupt

        if (eventId != null) {
            val event = ALL_EVENTS.find { it.id == eventId }
            if (event != null) {
                launchEvent(event)
                return
            }
        }

        // Filter eligible events, but ignore urgency for testing so we always get something
        val eligible = ALL_EVENTS.filter { it.id != "celebrate" }
        if (eligible.isEmpty()) return

        val event = eligible.random()
        launchEvent(event)
    }

    /** Tap-to-dismiss: end the current event early. */
    fun dismissEvent() {
        activeEvent = null
        eventProgress = 0f
    }

    private fun tryRollEvent() {
        if (activeEvent != null) return

        val now = System.currentTimeMillis()
        if (now - lastEventTime < hardCooldownMs) return

        // Filter eligible events
        val eligible = ALL_EVENTS.filter { event ->
            currentVisualState in event.applicableStates &&
                currentUrgency in event.urgencyRange &&
                event.id != "celebrate" // celebrate is deterministic, not random
        }
        if (eligible.isEmpty()) return

        // 10% chance
        if (Math.random() > 0.10) return

        val event = eligible.random()
        launchEvent(event)
    }

    private fun launchEvent(event: BlobEvent) {
        activeEvent = event
        eventProgress = 0f
        lastEventTime = System.currentTimeMillis()

        scope.launch {
            animateEvent(event)
        }
    }

    private suspend fun animateEvent(event: BlobEvent) {
        val steps = 60
        val stepMs = event.durationMs / steps
        for (i in 0..steps) {
            if (activeEvent?.id != event.id) return // dismissed early
            eventProgress = i.toFloat() / steps
            delay(stepMs)
        }
        // Event finished naturally
        activeEvent = null
        eventProgress = 0f
    }
}
