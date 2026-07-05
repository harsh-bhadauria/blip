package com.raven.blip.ui.overlay.components.blob.events

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Sleepy event: 💤 floats upward with a gentle sine-wave drift.
 * Eligible during IDLE state with low urgency.
 */
@Composable
fun SleepyEvent(progress: Float) {
    // Main 💤 drifts upward
    val yOffset = -progress * 30f  // moves upward
    val xDrift = sin(progress * Math.PI.toFloat() * 2f) * 4f
    val alpha = when {
        progress < 0.15f -> progress / 0.15f        // fade in
        progress > 0.75f -> (1f - progress) / 0.25f  // fade out
        else -> 1f
    }

    Text(
        text = "💤",
        fontSize = 18.sp,
        modifier = Modifier.graphicsLayer {
            translationX = xDrift
            translationY = yOffset
            this.alpha = alpha
        }
    )

    // Smaller 💤 trails behind, offset in time
    if (progress > 0.2f) {
        val trailProgress = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
        val trailY = -trailProgress * 22f
        val trailX = sin(trailProgress * Math.PI.toFloat() * 2f + 1f) * 3f
        val trailAlpha = when {
            trailProgress < 0.2f -> trailProgress / 0.2f
            trailProgress > 0.7f -> (1f - trailProgress) / 0.3f
            else -> 0.7f
        }

        Text(
            text = "💤",
            fontSize = 12.sp,
            modifier = Modifier.graphicsLayer {
                translationX = trailX + 8f
                translationY = trailY + 6f
                this.alpha = trailAlpha
            }
        )
    }
}
