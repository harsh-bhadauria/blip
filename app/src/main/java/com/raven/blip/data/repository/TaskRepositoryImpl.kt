package com.raven.blip.data.repository

import com.raven.blip.data.local.TaskDao
import com.raven.blip.data.model.Task
import com.raven.blip.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> = dao.observeAll()

    override suspend fun addTask(text: String, dueAt: Long?) {
        dao.insert(Task(text = text, dueAt = dueAt))
    }

    override suspend fun toggleComplete(task: Task) {
        dao.update(task.copy(isCompleted = !task.isCompleted))
    }

    override suspend fun deleteTask(task: Task) {
        dao.delete(task)
    }

    override suspend fun clearCompletedTasks() {
        dao.deleteCompleted()
    }
}
