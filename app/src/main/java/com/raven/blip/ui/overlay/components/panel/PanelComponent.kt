package com.raven.blip.ui.overlay.components.panel

import com.raven.blip.ui.theme.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.blip.data.model.Task
import com.raven.blip.ui.overlay.OverlayViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

// ── Helpers ────────────────────────────────────────────────────────────────────

fun todayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun tomorrowStart() = todayStart() + 86_400_000L

/** Smallest angular difference between two degree values, result in [0, 180]. */
fun angleDifference(a: Float, b: Float): Float {
    val diff = ((a - b + 180f + 360f) % 360f) - 180f
    return abs(diff)
}

@Composable
fun PanelOverlay(viewModel: OverlayViewModel, onDismissed: () -> Unit) {
    val isVisible = viewModel.isPanelVisible
    val density   = LocalDensity.current

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val showPanel = entered && isVisible

    val panelAlpha by animateFloatAsState(
        targetValue = if (showPanel) 1f else 0f,
        animationSpec = tween(150),
        label = "panel_alpha",
        finishedListener = { if (it == 0f && !isVisible) onDismissed() }
    )
    val panelOffsetY by animateFloatAsState(
        targetValue = if (showPanel) 0f else 16f,
        animationSpec = tween(150),
        label = "panel_offset"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = panelAlpha
                translationY = with(density) { panelOffsetY.dp.toPx() }
            }
    ) {
        if (viewModel.isAddingTask) {
            val focusRequester = remember { FocusRequester() }
            AddTaskPanel(
                focusRequester = focusRequester,
                onConfirm = { text, dueAt -> viewModel.addTask(text, dueAt) },
                onDismiss = viewModel::exitAddMode
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(100.milliseconds)
                focusRequester.requestFocus()
            }
        } else {
            val tasks by viewModel.tasksFlow.collectAsState()
            TaskListPanel(
                modifier = Modifier.width(280.dp),
                tasks = tasks,
                headroom = 80.dp,
                onTaskComplete = viewModel::completeTask,
                onTaskDelete   = viewModel::deleteTask,
                onClearCompleted = viewModel::clearCompletedTasks,
                onDismiss = viewModel::collapse
            )
        }
    }
}


// ── Due time formatter ─────────────────────────────────────────────────────────

fun formatDueTime(dueAt: Long?, isCompleted: Boolean): String? {
    if (dueAt == null || isCompleted) return null
    val now           = System.currentTimeMillis()
    val todayStart    = todayStart()
    val tomorrowStart = tomorrowStart()
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return when {
        dueAt < todayStart    -> "overdue · ${SimpleDateFormat("MMM d", Locale.getDefault()).format(dueAt)}"
        dueAt < now           -> "overdue · ${timeFmt.format(dueAt)}"
        dueAt < tomorrowStart -> "due ${timeFmt.format(dueAt)}"
        else                  -> null
    }
}

// ── Add task panel ─────────────────────────────────────────────────────────────

enum class Deadline { NONE, TODAY, TOMORROW }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskPanel(
    focusRequester: FocusRequester,
    onConfirm: (String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var text     by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(Deadline.NONE) }

    fun confirm() {
        val dueAt = when (deadline) {
            Deadline.NONE     -> null
            Deadline.TODAY    -> todayStart()
            Deadline.TOMORROW -> tomorrowStart()
        }
        onConfirm(text, dueAt)
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PanelBg.copy(alpha = 0.95f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(BlobColor),
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) confirm() }),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text("what needs doing?", color = TextMuted, fontSize = 15.sp)
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Deadline.entries.filter { it != Deadline.NONE }.forEach { d ->
                        val label = if (d == Deadline.TODAY) "today" else "tomorrow"
                        val selected = deadline == d
                        FilterChip(
                            selected = selected,
                            onClick = { deadline = if (selected) Deadline.NONE else d },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChipSelected,
                                selectedLabelColor = BlobColor,
                                containerColor = Color.Transparent,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = ChipBorder,
                                selectedBorderColor = BlobColor,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Confirm button — Canvas checkmark matching completed task style
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (text.isNotBlank()) BlobColor else ChipSelected)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (text.isNotBlank()) confirm() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val active = text.isNotBlank()
                    Canvas(Modifier.size(14.dp)) {
                        val r  = size.minDimension / 2f
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val path = Path().apply {
                            moveTo(cx - r * 0.45f, cy)
                            lineTo(cx - r * 0.05f, cy + r * 0.42f)
                            lineTo(cx + r * 0.52f, cy - r * 0.38f)
                        }
                        drawPath(
                            path = path,
                            color = if (active) PanelBg else TextMuted,
                            style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }
}


// ── Task list panel ────────────────────────────────────────────────────────────

@Composable
fun TaskListPanel(
    modifier: Modifier = Modifier,
    tasks: List<Task>,
    headroom: androidx.compose.ui.unit.Dp = 0.dp,
    showAll: Boolean = false,
    onTaskComplete: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    onClearCompleted: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val pending   = tasks.filter { !it.isCompleted && (showAll || it.dueAt == null || it.dueAt < tomorrowStart()) }
    val completed = tasks.filter { it.isCompleted }

    var radialState by remember { mutableStateOf<RadialMenuState?>(null) }
    var panelCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val triggerPx = with(density) { 30.dp.toPx() }

    // Outer Box is NOT clipped — so the radial menu can bleed beyond the panel rect
    Box(
        modifier = modifier
            .onGloballyPositioned { panelCoords = it }
    ) {
        // Invisible clickable area for the headroom
        if (headroom > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headroom)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // ↓ Content pushed down by headroom — this creates space above the visible panel
        // so bubbles at 270° (straight up) are never clipped by the window boundary.
        Box(modifier = Modifier.padding(top = headroom).fillMaxWidth()) {
            // Clipped background (separate so radial overlay above isn't clipped)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelBg.copy(alpha = 0.92f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Absorb clicks so they don't fall through
                    )
            )

            if (pending.isEmpty() && completed.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("nothing to do!", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(pending, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { onTaskComplete(task) },
                            onLongPress = { rowCoords, touchPos ->
                                val panelLocal = panelCoords?.localPositionOf(rowCoords, touchPos) ?: touchPos
                                radialState = RadialMenuState(task, panelLocal)
                            },
                            onDragUpdate = { dx, dy ->
                                radialState = radialState?.copy(dragX = dx, dragY = dy)
                            },
                            onDragEnd = { triggered ->
                                if (triggered) radialState?.let { onTaskDelete(it.task) }
                                radialState = null
                            }
                        )
                    }
                    if (completed.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "done",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    "clear all",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onClearCompleted
                                    )
                                )
                            }
                        }
                        items(completed, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggle = { onTaskComplete(task) },
                                onLongPress = { rowCoords, touchPos ->
                                    val panelLocal = panelCoords?.localPositionOf(rowCoords, touchPos) ?: touchPos
                                    radialState = RadialMenuState(task, panelLocal)
                                },
                                onDragUpdate = { dx, dy ->
                                    radialState = radialState?.copy(dragX = dx, dragY = dy)
                                },
                                onDragEnd = { triggered ->
                                    if (triggered) radialState?.let { onTaskDelete(it.task) }
                                    radialState = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // Radial menu — sibling of content box, uses the full outer Box including headroom
        radialState?.let { state ->
            RadialActionMenu(
                center = state.center,
                dragX  = state.dragX,
                dragY  = state.dragY,
                triggerDistancePx = triggerPx
            )
        }
    }
}


// ── Task row ───────────────────────────────────────────────────────────────────

@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onLongPress: (rowCoords: LayoutCoordinates, touchPos: Offset) -> Unit,
    onDragUpdate: (dx: Float, dy: Float) -> Unit,
    onDragEnd: (triggered: Boolean) -> Unit
) {
    val density = LocalDensity.current
    val haptic  = LocalHapticFeedback.current
    val triggerPx = with(density) { 30.dp.toPx() }

    var rowCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowCoords = it }
            .pointerInput(task.id) {
                val longPressMs = viewConfiguration.longPressTimeoutMillis
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Wait for lift — if it happens within longPressMs it's a tap
                    val lifted = withTimeoutOrNull(longPressMs) {
                        var up = false
                        while (!up) {
                            val evt    = awaitPointerEvent()
                            val change = evt.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) up = true
                        }
                    }

                    if (lifted != null) {
                        onToggle()
                    } else {
                        // Long press confirmed
                        rowCoords?.let { onLongPress(it, down.position) }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        var cumX = 0f; var cumY = 0f
                        var prevPos = down.position
                        drag(down.id) { change ->
                            cumX += change.position.x - prevPos.x
                            cumY += change.position.y - prevPos.y
                            prevPos = change.position
                            onDragUpdate(cumX, cumY)
                            change.consume()
                        }

                        val dist  = sqrt(cumX * cumX + cumY * cumY)
                        val angle = (atan2(cumY, cumX) * 180f / PI).toFloat()
                        val triggered = dist > triggerPx &&
                                actionSlots.any { angleDifference(angle, it.angleDeg) < 40f }
                        onDragEnd(triggered)
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox via Canvas for perfect centering
        Canvas(modifier = Modifier.size(16.dp)) {
            val r  = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            if (task.isCompleted) {
                drawCircle(color = CheckGreen, radius = r)
                val path = Path().apply {
                    moveTo(cx - r * 0.35f, cy)
                    lineTo(cx - r * 0.05f, cy + r * 0.32f)
                    lineTo(cx + r * 0.42f, cy - r * 0.28f)
                }
                drawPath(path, PanelBg, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawCircle(color = CheckUnchecked, radius = r - 0.8.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.text,
                color = if (task.isCompleted) TextMuted else TextPrimary,
                fontSize = 14.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val dueLabel = remember(task.dueAt, task.isCompleted) {
                formatDueTime(task.dueAt, task.isCompleted)
            }
            if (dueLabel != null) {
                Text(
                    text = dueLabel,
                    color = if (task.dueAt != null && task.dueAt < System.currentTimeMillis())
                        DotOverdue else TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

