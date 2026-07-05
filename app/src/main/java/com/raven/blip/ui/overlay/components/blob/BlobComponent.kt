package com.raven.blip.ui.overlay.components.blob

import com.raven.blip.ui.theme.*
import com.raven.blip.domain.model.BlobVisualState
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.raven.blip.ui.overlay.OverlayViewModel
import com.raven.blip.ui.overlay.components.blob.events.BloomEvent
import com.raven.blip.ui.overlay.components.blob.events.CelebrateEvent
import com.raven.blip.ui.overlay.components.blob.events.SleepyEvent
import kotlin.math.roundToInt

@Composable
fun BlobOverlay(viewModel: OverlayViewModel) {
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
    val visualState = viewModel.blobVisualState
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "offsetX")

    // ── Event system ────────────────────────────────────────────────────
    val eventController = remember { BlobEventController() }
    val activeEvent = eventController.activeEvent

    LaunchedEffect(visualState, urgency) {
        eventController.updateState(visualState, urgency)
    }

    // Drive the event timer
    LaunchedEffect(Unit) {
        eventController.startEventLoop()
    }

    // Completion triggers the celebrate event deterministically
    LaunchedEffect(viewModel.completionTrigger) {
        if (viewModel.completionTrigger > 0) {
            eventController.triggerCelebration()
        }
    }

    // morphOut animates the shader blob away when an event is active
    val morphOut by animateFloatAsState(
        targetValue = if (activeEvent != null) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "morphOut"
    )

    // ── Time loop for shader ────────────────────────────────────────────
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { ms -> time = ms / 1000f }
        }
    }

    // ── Shader setup ────────────────────────────────────────────────────
    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(BLIP_SHADER)
        } else null
    }

    val density = LocalDensity.current
    val sizePx = remember(density) { with(density) { 44.dp.toPx() } }

    val brush = remember(shader, time, urgency, sizePx, visualState, morphOut) {
        shader?.apply {
            setFloatUniform("resolution", sizePx, sizePx)
            setFloatUniform("time", time)
            setFloatUniform("urgency", urgency)
            setFloatUniform("visualState", visualState.ordinal.toFloat())
            setFloatUniform("morphOut", morphOut)
        }
        if (shader != null) ShaderBrush(shader) else null
    }

    // ── Rendering ───────────────────────────────────────────────────────
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
                    onDragCancel = { offsetX = 0f }
                ) { _, dragAmount ->
                    offsetX += dragAmount
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    // If an event is playing, tap dismisses it; otherwise normal click
                    if (activeEvent != null) {
                        eventController.dismissEvent()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (activeEvent == null) onLongClick()
                }
            )

        // Layer 1: Shader blob (always rendered, morphOut makes it invisible during events)
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
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        alpha = (1f - morphOut).coerceIn(0f, 1f)
                    }
                    .background(BlobColor)
            )
        }

        // Layer 2: Event composable (rendered on top when active)
        if (activeEvent != null && morphOut > 0.8f) {
            val eventProgress = eventController.eventProgress
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                when (activeEvent.id) {
                    "sleepy" -> SleepyEvent(progress = eventProgress)
                    "celebrate" -> CelebrateEvent(progress = eventProgress)
                    "bloom" -> BloomEvent(progress = eventProgress)
                }
            }
        }
    }
}
