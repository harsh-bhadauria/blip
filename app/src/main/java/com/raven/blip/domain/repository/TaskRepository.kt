package com.raven.blip.domain.repository

import com.raven.blip.data.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun addTask(text: String, dueAt: Long? = null)
    suspend fun toggleComplete(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun clearCompletedTasks()
}
