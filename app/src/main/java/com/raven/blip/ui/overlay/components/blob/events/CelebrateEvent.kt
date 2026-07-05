package com.raven.blip.ui.overlay.components.blob.events

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Celebration event: 🎉 pops in with a bounce, then confetti particles
 * radiate outward. Triggered deterministically on task completion.
 */
@Composable
fun CelebrateEvent(progress: Float) {
    // Emoji pops in with a bounce during first 30%
    val emojiScale = when {
        progress < 0.1f -> progress / 0.1f * 1.3f  // overshoot
        progress < 0.2f -> 1.3f - (progress - 0.1f) / 0.1f * 0.3f  // settle
        progress > 0.8f -> (1f - progress) / 0.2f  // fade out
        else -> 1f
    }
    val emojiAlpha = if (progress > 0.85f) (1f - progress) / 0.15f else 1f

    Text(
        text = "🎉",
        fontSize = 20.sp,
        modifier = Modifier.graphicsLayer {
            scaleX = emojiScale
            scaleY = emojiScale
            alpha = emojiAlpha
        }
    )

    // Confetti particles burst outward
    if (progress > 0.05f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = ((progress - 0.05f) / 0.95f).coerceIn(0f, 1f)
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            val colors = listOf(
                Color(0xFFFF6B6B), // coral
                Color(0xFFFFD93D), // gold
                Color(0xFF6BCB77), // green
                Color(0xFF4D96FF), // blue
                Color(0xFFFF9FF3), // pink
                Color(0xFFA29BFE), // lavender
            )

            for (i in 0 until 10) {
                val angle = (i * Math.PI * 2 / 10).toFloat() + (i * 0.3f)
                val distance = p * maxRadius * (0.8f + (i % 3) * 0.15f)
                val x = center.x + cos(angle) * distance
                val y = center.y + sin(angle) * distance
                val alpha = when {
                    p < 0.3f -> 1f
                    else -> (1f - (p - 0.3f) / 0.7f).coerceIn(0f, 1f)
                }
                val radius = 2.5.dp.toPx() * (1f - p * 0.6f)
                drawCircle(
                    color = colors[i % colors.size],
                    radius = radius,
                    center = Offset(x, y),
                    alpha = alpha
                )
            }
        }
    }
}
