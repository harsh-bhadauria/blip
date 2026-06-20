package com.raven.blip.domain.model

enum class OverlayCorner {
    BOTTOM_END,
    BOTTOM_START,
    TOP_END,
    TOP_START
}

data class AppSettings(
    val corner: OverlayCorner = OverlayCorner.BOTTOM_END,
    val bubbleIntervalMs: Long = 15 * 60 * 1000L,
    val quietHoursStart: Int? = null, // Hour of day 0-23
    val quietHoursEnd: Int? = null,
    val lastCompletedAt: Long = 0L,
    val skinId: String = "default"
)
