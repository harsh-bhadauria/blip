package com.raven.blip.domain.model

/**
 * Defines a random event that can play on the blob.
 *
 * @param id Unique identifier, also used to look up the composable renderer.
 * @param applicableStates Which visual states can trigger this event.
 * @param urgencyRange The urgency window in which this event is eligible.
 * @param durationMs How long the event's compose animation plays (excluding morph transitions).
 */
data class BlobEvent(
    val id: String,
    val applicableStates: Set<BlobVisualState>,
    val urgencyRange: ClosedFloatingPointRange<Float> = 0f..1f,
    val durationMs: Long = 2500,
)
