package com.example.efscheduler.models

import android.content.Context
import com.example.efscheduler.models.notification.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val context: Context) {
    private val database = TaskDatabase.getInstance(context)
    private val taskDao = database.taskDao()

    // Expose tasks as Flow, mapping from Entity to Item
    val tasksFlow: Flow<List<TaskItem>> = taskDao.getAllTasks()
        .map { entities ->
            entities.map { TaskItem(it.id, it.name, it.timestamp) }
        }

    // Add a new task (or update existing) – cancel old notification if editing
    suspend fun addTask(task: TaskItem, oldTaskId: String? = null) {
        oldTaskId?.let { id ->
            taskDao.getAllTasks().collect { entities ->
                entities.find { it.id == id }?.let { oldEntity ->
                    NotificationScheduler.cancelTaskNotification(
                        context,
                        TaskItem(oldEntity.id, oldEntity.name, oldEntity.timestamp, oldEntity.reminderMinutes)
                    )
                    // Also cancel reminder if exists
                    if (oldEntity.reminderMinutes > 0) {
                        NotificationScheduler.cancelReminderNotification(
                            context,
                            TaskItem(oldEntity.id, oldEntity.name, oldEntity.timestamp, oldEntity.reminderMinutes)
                        )
                    }
                }
            }
        }
        taskDao.insertTask(TaskEntity(task.id, task.name, task.timestamp, task.reminderMinutes))
        NotificationScheduler.scheduleTaskNotification(context, task)
        if (task.reminderMinutes > 0) {
            NotificationScheduler.scheduleReminderNotification(context, task)
        }
    }

    suspend fun deleteTask(task: TaskItem) {
        NotificationScheduler.cancelTaskNotification(context, task)
        if (task.reminderMinutes > 0) {
            NotificationScheduler.cancelReminderNotification(context, task)
        }
        taskDao.deleteTask(TaskEntity(task.id, task.name, task.timestamp, task.reminderMinutes))
    }

}