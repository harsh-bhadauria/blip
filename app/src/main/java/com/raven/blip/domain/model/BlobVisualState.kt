package com.raven.blip.domain.model

/**
 * Describes the blob's visual/emotional state, separate from the UI mode (OverlayState).
 * The renderer uses this to drive shape deformation, animation speed, and event eligibility.
 */
enum class BlobVisualState {
    IDLE,         // Chilling, gentle breathing
    NUDGING,      // Trying to get attention (pre-bubble anticipation)
    SPEAKING,     // Bubble is visible, blob is "talking"
    LISTENING,    // Panel is open, waiting for input
    THINKING,     // User is adding a task
    CELEBRATING,  // Task just completed
    HIDING        // Swiped away temporarily
}
