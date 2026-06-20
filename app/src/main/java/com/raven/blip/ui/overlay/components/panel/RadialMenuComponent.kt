package com.raven.blip.ui.overlay.components.panel

import com.raven.blip.ui.theme.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.blip.data.model.Task
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class BubbleSlot(val angleDeg: Float, val icon: String, val activeColor: Color)
val actionSlots = listOf(
    BubbleSlot(270f, "🗑", DeleteBubble)
    // Future: BubbleSlot(315f, "⏰", SnoozeColor), BubbleSlot(225f, "✏️", EditColor)
)

data class RadialMenuState(
    val task: Task,
    val center: Offset,   // panel-local coordinates
    val dragX: Float = 0f,
    val dragY: Float = 0f
)

@Composable
fun RadialActionMenu(
    center: Offset,
    dragX: Float,
    dragY: Float,
    triggerDistancePx: Float
) {
    val density      = LocalDensity.current
    val orbitPx      = with(density) { 60.dp.toPx() }  // center-to-bubble distance
    val bubbleSizePx = with(density) { 44.dp.toPx() }

    val dragDist     = sqrt(dragX * dragX + dragY * dragY)
    val dragAngleDeg = (atan2(dragY, dragX) * 180f / PI).toFloat()

    actionSlots.forEach { slot ->
        val rad     = slot.angleDeg * PI.toFloat() / 180f
        val bubbleCx = center.x + cos(rad) * orbitPx
        val bubbleCy = center.y + sin(rad) * orbitPx

        val angleDiff = angleDifference(dragAngleDeg, slot.angleDeg)
        val isActive  = dragDist > triggerDistancePx && angleDiff < 40f

        val scale by animateFloatAsState(
            targetValue = if (isActive) 1.25f else 1f,
            animationSpec = tween(80),
            label = "radial_scale_${slot.angleDeg}"
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (bubbleCx - bubbleSizePx / 2f).roundToInt(),
                        (bubbleCy - bubbleSizePx / 2f).roundToInt()
                    )
                }
                .size(with(density) { bubbleSizePx.toDp() })
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f
                drawCircle(color = if (isActive) slot.activeColor else BubbleBg, radius = r)
                drawCircle(
                    color = if (isActive) slot.activeColor else BlobColor,
                    radius = r - 1.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            Text(slot.icon, fontSize = 18.sp)
        }
    }
}

