package com.raven.blip.ui.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raven.blip.data.model.Task
import com.raven.blip.domain.model.BlobVisualState
import com.raven.blip.domain.model.OverlayState
import com.raven.blip.domain.repository.SettingsRepository
import com.raven.blip.domain.repository.SkinRepository
import com.raven.blip.domain.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverlayViewModel(
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val skinRepository: SkinRepository
) : ViewModel() {

    var overlayState by mutableStateOf(OverlayState.COLLAPSED)
        private set

    var completionTrigger by mutableIntStateOf(0)
        private set

    var testEventTrigger by mutableIntStateOf(0)
        private set

    var testEventTriggerId by mutableStateOf<String?>(null)
        private set

    fun triggerTestEvent(eventId: String? = null) {
        testEventTriggerId = eventId
        testEventTrigger++
    }

    val isAddingTask get() = overlayState == OverlayState.ADDING_TASK
    val isPanelVisible get() = overlayState == OverlayState.EXPANDED || isAddingTask

    /** Visual/emotional state for the blob renderer, derived from UI state. */
    val blobVisualState: BlobVisualState get() = when {
        overlayState == OverlayState.BUBBLE -> BlobVisualState.SPEAKING
        overlayState == OverlayState.ADDING_TASK -> BlobVisualState.THINKING
        overlayState == OverlayState.EXPANDED -> BlobVisualState.LISTENING
        else -> BlobVisualState.IDLE
    }

    var activeBubbleTask by mutableStateOf<Task?>(null)
        private set

    var activeBubbleText by mutableStateOf("")
        private set

    val urgency: Float get() = computeUrgency(tasksFlow.value.filter { !it.isCompleted })
    val recentActivity: Float get() = computeRecentActivity(settingsFlow.value.lastCompletedAt)

    private var lastBubbleTaskId: Int? = null
    private var lastBubbleShownAt: Long = 0L

    val tasksFlow: StateFlow<List<Task>> = repository.observeTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val settingsFlow = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.raven.blip.domain.model.AppSettings()
        )

    private var lastCollapseTime = 0L

    fun toggleOverlay() {
        if (System.currentTimeMillis() - lastCollapseTime < 300) return
        overlayState = when (overlayState) {
            OverlayState.COLLAPSED -> OverlayState.EXPANDED
            OverlayState.EXPANDED -> OverlayState.COLLAPSED
            OverlayState.BUBBLE -> OverlayState.EXPANDED
            OverlayState.ADDING_TASK -> OverlayState.COLLAPSED
        }
    }

    fun enterAddMode() {
        // Always open the panel first, then switch to input mode
        overlayState = OverlayState.ADDING_TASK
    }

    fun exitAddMode() {
        overlayState = OverlayState.EXPANDED
    }

    fun collapse() {
        overlayState = OverlayState.COLLAPSED
        lastCollapseTime = System.currentTimeMillis()
    }

    fun addTask(text: String, dueAt: Long? = null) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addTask(text.trim(), dueAt)
        }
        exitAddMode()
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            repository.toggleComplete(task)
            if (!task.isCompleted) { // means it just got completed
                settingsRepository.updateLastCompletedAt(System.currentTimeMillis())
                completionTrigger++
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
        }
    }

    fun showNextBubble() {
        val settings = settingsFlow.value
        val startHour = settings.quietHoursStart
        val endHour = settings.quietHoursEnd
        
        if (startHour != null && endHour != null) {
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val isQuietHours = if (startHour <= endHour) {
                currentHour in startHour until endHour
            } else {
                currentHour >= startHour || currentHour < endHour
            }
            if (isQuietHours) return // Don't show bubble during quiet hours
        }

        val now = System.currentTimeMillis()
        val pending = tasksFlow.value.filter { !it.isCompleted }
        
        if (pending.isEmpty()) return

        // cooldown scales with interval and task count
        val cooldownMs = settingsFlow.value.bubbleIntervalMs * maxOf(pending.size, 2)

        val candidates = pending
            .filter { it.id != lastBubbleTaskId || (now - lastBubbleShownAt) > cooldownMs }
            .sortedBy { it.dueAt ?: Long.MAX_VALUE }

        activeBubbleTask = candidates.firstOrNull() ?: pending.random()
        lastBubbleTaskId = activeBubbleTask!!.id
        lastBubbleShownAt = now
        
        viewModelScope.launch {
            val lines = skinRepository.getVibeLines(settingsFlow.value.skinId)
            val region = getMoodRegion(urgency)
            val templates = lines[region]
            
            val formatted = if (templates.isNullOrEmpty()) {
                activeBubbleTask!!.text
            } else {
                templates.random().replace("{task}", activeBubbleTask!!.text)
            }
            activeBubbleText = formatted
            overlayState = OverlayState.BUBBLE
        }
    }

    private fun computeUrgency(pending: List<Task>): Float {
        if (pending.isEmpty()) return 0f

        val now = System.currentTimeMillis()
        val datedUrgencies = pending
            .mapNotNull { it.dueAt }
            .map { dueAt ->
                val hoursUntilDue = (dueAt - now) / 3_600_000f
                (1f - hoursUntilDue / 24f).coerceIn(0f, 1f)
            }

        val undatedUrgency = (pending.count { it.dueAt == null } / 6f).coerceIn(0f, 0.5f)

        return ((datedUrgencies.maxOrNull() ?: 0f) + undatedUrgency).coerceIn(0f, 1f)
    }

    private fun computeRecentActivity(lastCompletedAt: Long): Float {
        if (lastCompletedAt == 0L) return 0f
        val elapsedMs = System.currentTimeMillis() - lastCompletedAt
        val decayMs = 30 * 60 * 1000f
        return (1f - elapsedMs / decayMs).coerceIn(0f, 1f)
    }

    private fun getMoodRegion(urgency: Float) = when {
        urgency < 0.2f -> "idle"
        urgency < 0.5f -> "light"
        urgency < 0.8f -> "busy"
        else           -> "overwhelmed"
    }
}
