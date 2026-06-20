package com.raven.blip.ui.overlay.components.blob

import com.raven.blip.ui.theme.*
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.blip.ui.overlay.OverlayViewModel
import kotlin.math.roundToInt

@Composable
fun BlobOverlay(viewModel: OverlayViewModel) {
    val haptic = LocalHapticFeedback.current

    Box(contentAlignment = Alignment.BottomEnd) {
        BlipBlob(
            viewModel = viewModel,
            onClick = viewModel::toggleOverlay,
            onLongClick = viewModel::enterAddMode
        )
    }
}


@Composable
fun BlipBlob(viewModel: OverlayViewModel, onClick: () -> Unit, onLongClick: () -> Unit) {
    var isTemporarilyHidden by remember { mutableStateOf(false) }

    LaunchedEffect(isTemporarilyHidden) {
        if (isTemporarilyHidden) {
            viewModel.collapse()

            kotlinx.coroutines.delay(5_000)
            isTemporarilyHidden = false
        }
    }

    if (isTemporarilyHidden) {
        Box(modifier = Modifier.size(1.dp))
        return
    }

    val urgency = viewModel.urgency
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "offsetX")

    val completionAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(viewModel.completionTrigger) {
        if (viewModel.completionTrigger > 0) {
            completionAnim.snapTo(0f)
            completionAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
            )
        }
    }
    val isAnimatingCompletion = completionAnim.value > 0f && completionAnim.value < 1f

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { ms ->
                time = ms / 1000f
            }
        }
    }

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(BLIP_SHADER)
        } else null
    }

    val density = LocalDensity.current
    val sizePx = remember(density) { with(density) { 44.dp.toPx() } }

    val brush = remember(shader, time, urgency, sizePx) {
        shader?.apply {
            setFloatUniform("resolution", sizePx, sizePx)
            setFloatUniform("time", time)
            setFloatUniform("urgency", urgency)
        }
        if (shader != null) ShaderBrush(shader) else null
    }

    Box(
        modifier = Modifier.padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .size(44.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 100f) {
                            isTemporarilyHidden = true
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    }
                ) { change, dragAmount ->
                    offsetX += dragAmount
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )

        if (isAnimatingCompletion) {
            Box(modifier = modifier.background(Color.White)) {
                Text(
                    text = "🎉",
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p = completionAnim.value
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f
                    for (i in 0 until 8) {
                        val angle = (i * Math.PI * 2 / 8).toFloat()
                        val distance = p * maxRadius
                        val x = center.x + kotlin.math.cos(angle) * distance
                        val y = center.y + kotlin.math.sin(angle) * distance
                        val alpha = if (p < 0.5f) 1f else 1f - (p - 0.5f) * 2f
                        val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green)
                        drawCircle(
                            color = colors[i % colors.size],
                            radius = 3.dp.toPx() * (1f - p),
                            center = Offset(x, y),
                            alpha = alpha
                        )
                    }
                }
            }
        } else {
            if (brush != null) {
                Box(modifier = modifier.background(brush as androidx.compose.ui.graphics.Brush))
            } else {
                // Fallback for API < 33
                val infiniteTransition = rememberInfiniteTransition(label = "blip_pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue  = 1.06f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                    label = "blip_scale"
                )
                Box(
                    modifier = modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = 0.9f }
                        .background(BlobColor)
                )
            }
        }
    }
}

