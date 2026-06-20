package com.raven.blip.ui.overlay.components.bubble

import com.raven.blip.ui.theme.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.blip.domain.model.OverlayState
import com.raven.blip.ui.overlay.OverlayViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ThoughtBubble(text: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
            .background(PanelBg)
            .clickable { onDismiss() }
            .padding(14.dp)
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 14.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
fun BubbleOverlay(viewModel: OverlayViewModel, onDismissed: () -> Unit) {
    val showBubble = viewModel.overlayState == OverlayState.BUBBLE && viewModel.activeBubbleTask != null

    val bubbleAlpha by animateFloatAsState(
        targetValue = if (showBubble) 1f else 0f,
        animationSpec = tween(150),
        label = "bubble_alpha",
        finishedListener = { if (it == 0f && !showBubble) onDismissed() }
    )

    Box(modifier = Modifier.graphicsLayer { alpha = bubbleAlpha }) {
        viewModel.activeBubbleTask?.let { task ->
            LaunchedEffect(task) {
                if (viewModel.overlayState == OverlayState.BUBBLE) {
                    kotlinx.coroutines.delay(5_000L.milliseconds)
                    if (viewModel.overlayState == OverlayState.BUBBLE) {
                        viewModel.collapse()
                    }
                }
            }
            ThoughtBubble(
                text = viewModel.activeBubbleText,
                onDismiss = viewModel::collapse
            )
        }
    }
}

