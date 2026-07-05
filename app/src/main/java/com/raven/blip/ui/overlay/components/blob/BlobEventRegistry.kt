package com.raven.blip.ui.overlay.components.blob

import com.raven.blip.domain.model.BlobEvent
import com.raven.blip.domain.model.BlobVisualState

/** All available random events and their eligibility rules. */
val ALL_EVENTS = listOf(
    BlobEvent(
        id = "sleepy",
        applicableStates = setOf(BlobVisualState.IDLE),
        urgencyRange = 0f..0.3f,
        durationMs = 3000
    ),
    BlobEvent(
        id = "celebrate",
        applicableStates = setOf(BlobVisualState.CELEBRATING),
        urgencyRange = 0f..1f,
        durationMs = 1500
    ),
    BlobEvent(
        id = "bloom",
        applicableStates = setOf(BlobVisualState.IDLE),
        urgencyRange = 0f..1f,
        durationMs = 2500
    )
)
