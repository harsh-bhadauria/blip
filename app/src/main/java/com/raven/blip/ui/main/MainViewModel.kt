package com.raven.blip.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raven.blip.data.model.Task
import com.raven.blip.domain.model.AppSettings
import com.raven.blip.domain.model.OverlayCorner
import com.raven.blip.domain.repository.SettingsRepository
import com.raven.blip.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val tasksFlow: StateFlow<List<Task>> = taskRepository.observeTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val settingsFlow: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun updateCorner(corner: OverlayCorner) {
        viewModelScope.launch {
            settingsRepository.updateCorner(corner)
        }
    }

    fun updateBubbleInterval(intervalMs: Long) {
        viewModelScope.launch {
            settingsRepository.updateBubbleInterval(intervalMs)
        }
    }

    fun updateQuietHours(start: Int?, end: Int?) {
        viewModelScope.launch {
            settingsRepository.updateQuietHours(start, end)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleComplete(task)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            taskRepository.clearCompletedTasks()
        }
    }

    fun injectTestTasks() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val tasks = listOf(
                Pair("Buy groceries", now - dayMs), // Overdue
                Pair("Finish Blip UI", now + 1000 * 60 * 60), // Due in 1 hour
                Pair("Call mom", now + dayMs), // Due tomorrow
                Pair("Brainstorm new features", null) // No due date
            )
            tasks.forEach { (text, dueAt) -> taskRepository.addTask(text, dueAt) }
        }
    }
}
