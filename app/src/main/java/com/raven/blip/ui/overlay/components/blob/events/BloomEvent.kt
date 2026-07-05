package com.raven.blip.ui.overlay.components.blob.events

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bloom event: a central 🌸 scales up, and a few small 🌸 drift outward.
 * Eligible during IDLE state with any urgency.
 */
@Composable
fun BloomEvent(progress: Float) {
    // Central flower scales up and gently rotates
    val centerScale = when {
        progress < 0.2f -> progress / 0.2f
        progress > 0.8f -> (1f - progress) / 0.2f
        else -> 1f + sin(progress * Math.PI.toFloat()) * 0.1f // slight pulsing
    }
    val centerAlpha = when {
        progress < 0.1f -> progress / 0.1f
        progress > 0.85f -> (1f - progress) / 0.15f
        else -> 1f
    }
    val centerRot = progress * 45f // slowly rotates 45 degrees

    Text(
        text = "🌸",
        fontSize = 24.sp,
        modifier = Modifier.graphicsLayer {
            scaleX = centerScale
            scaleY = centerScale
            alpha = centerAlpha
            rotationZ = centerRot
        }
    )

    // Smaller petals drift outward
    if (progress > 0.15f) {
        val petalProgress = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
        for (i in 0 until 4) {
            val angle = (i * Math.PI * 2 / 4).toFloat() + (progress * 0.5f) // slow spiral
            val distance = petalProgress * 30f // drift outward
            val x = cos(angle) * distance
            val y = sin(angle) * distance
            
            val petalScale = 0.5f * (1f - petalProgress * 0.3f)
            val petalAlpha = when {
                petalProgress < 0.2f -> petalProgress / 0.2f
                petalProgress > 0.6f -> (1f - (petalProgress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                else -> 1f
            }
            val petalRot = petalProgress * 90f + (i * 45f)

            Text(
                text = "🌸",
                fontSize = 24.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = x
                    translationY = y
                    scaleX = petalScale
                    scaleY = petalScale
                    alpha = petalAlpha
                    rotationZ = petalRot
                }
            )
        }
    }
}
