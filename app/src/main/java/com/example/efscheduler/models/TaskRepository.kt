package com.example.efscheduler.models

import android.content.Context
import android.util.Log
import com.example.efscheduler.models.notification.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(private val context: Context) {
    private val database = TaskDatabase.getInstance(context)
    private val taskDao = database.taskDao()

    companion object {
        private const val TAG = "EFSchedulerRepo"
    }

    val tasksFlow: Flow<List<TaskItem>> = taskDao.getAllTasks()
        .map { entities ->
            entities.map {
                TaskItem(
                    id = it.id,
                    name = it.name,
                    timestamp = it.timestamp,
                    reminderMinutes = it.reminderMinutes
                )
            }
        }

    suspend fun addTask(task: TaskItem, oldTaskId: String? = null) {
        val isEdit = oldTaskId != null

        Log.d(
            TAG,
            "${if (isEdit) "EDIT" else "CREATE"} requested: " +
                    "newId=${task.id}, oldId=$oldTaskId, " +
                    "name=${task.name}, timestamp=${task.timestamp}, reminder=${task.reminderMinutes}"
        )

        oldTaskId?.let { id ->
            val oldEntity = taskDao.getTaskById(id)

            if (oldEntity != null) {
                Log.d(
                    TAG,
                    "EDIT old task found: " +
                            "id=${oldEntity.id}, name=${oldEntity.name}, " +
                            "timestamp=${oldEntity.timestamp}, reminder=${oldEntity.reminderMinutes}"
                )

                val oldTask = TaskItem(
                    id = oldEntity.id,
                    name = oldEntity.name,
                    timestamp = oldEntity.timestamp,
                    reminderMinutes = oldEntity.reminderMinutes
                )

                NotificationScheduler.cancelTaskNotification(context, oldTask)
                Log.d(TAG, "EDIT cancelled old main notification: id=${oldTask.id}")

                if (oldTask.reminderMinutes > 0) {
                    NotificationScheduler.cancelReminderNotification(context, oldTask)
                    Log.d(TAG, "EDIT cancelled old before-reminder: id=${oldTask.id}")
                }
            } else {
                Log.w(TAG, "EDIT requested but old task was not found: oldId=$id")
            }
        }

        taskDao.insertTask(
            TaskEntity(
                id = task.id,
                name = task.name,
                timestamp = task.timestamp,
                reminderMinutes = task.reminderMinutes
            )
        )

        Log.d(
            TAG,
            "${if (isEdit) "EDIT" else "CREATE"} saved to Room: " +
                    "id=${task.id}, name=${task.name}, timestamp=${task.timestamp}, reminder=${task.reminderMinutes}"
        )

        NotificationScheduler.scheduleTaskNotification(context, task)
        Log.d(TAG, "${if (isEdit) "EDIT" else "CREATE"} scheduled main notification: id=${task.id}")

        if (task.reminderMinutes > 0) {
            NotificationScheduler.scheduleReminderNotification(context, task)
            Log.d(
                TAG,
                "${if (isEdit) "EDIT" else "CREATE"} scheduled before-reminder: " +
                        "id=${task.id}, minutes=${task.reminderMinutes}"
            )
        }

        logAllTasks("after ${if (isEdit) "edit" else "create"}")
    }

    suspend fun deleteTask(task: TaskItem) {
        Log.d(
            TAG,
            "DELETE requested: id=${task.id}, name=${task.name}, " +
                    "timestamp=${task.timestamp}, reminder=${task.reminderMinutes}"
        )

        NotificationScheduler.cancelTaskNotification(context, task)
        Log.d(TAG, "DELETE cancelled main notification: id=${task.id}")

        if (task.reminderMinutes > 0) {
            NotificationScheduler.cancelReminderNotification(context, task)
            Log.d(TAG, "DELETE cancelled before-reminder: id=${task.id}")
        }

        taskDao.deleteTask(
            TaskEntity(
                id = task.id,
                name = task.name,
                timestamp = task.timestamp,
                reminderMinutes = task.reminderMinutes
            )
        )

        Log.d(TAG, "DELETE removed from Room: id=${task.id}, name=${task.name}")

        logAllTasks("after delete")
    }

    suspend fun exportTasksToJson(): String {
        val tasks = taskDao.getAllTasksOnce()
        val tasksArray = JSONArray()

        Log.d(TAG, "EXPORT requested. Task count=${tasks.size}")

        tasks.forEach { task ->
            Log.d(
                TAG,
                "EXPORT task: id=${task.id}, name=${task.name}, " +
                        "timestamp=${task.timestamp}, reminder=${task.reminderMinutes}"
            )

            tasksArray.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("name", task.name)
                    put("timestamp", task.timestamp)
                    put("reminderMinutes", task.reminderMinutes)
                }
            )
        }

        val json = JSONObject().apply {
            put("schemaVersion", 1)
            put("app", "EFScheduler")
            put("exportedAt", System.currentTimeMillis())
            put("tasks", tasksArray)
        }.toString(2)

        Log.d(TAG, "EXPORT complete")

        return json
    }

    suspend fun importTasksFromJson(
        json: String,
        replaceExisting: Boolean = false
    ) {
        Log.d(TAG, "IMPORT requested. replaceExisting=$replaceExisting")

        val root = JSONObject(json)
        val tasksArray = root.getJSONArray("tasks")
        val importedTasks = mutableListOf<TaskEntity>()

        for (i in 0 until tasksArray.length()) {
            val taskJson = tasksArray.getJSONObject(i)

            val task = TaskEntity(
                id = taskJson.getString("id"),
                name = taskJson.getString("name"),
                timestamp = taskJson.getLong("timestamp"),
                reminderMinutes = taskJson.optInt("reminderMinutes", 0)
            )

            Log.d(
                TAG,
                "IMPORT parsed task: id=${task.id}, name=${task.name}, " +
                        "timestamp=${task.timestamp}, reminder=${task.reminderMinutes}"
            )

            importedTasks.add(task)
        }

        if (replaceExisting) {
            Log.d(TAG, "IMPORT replacing existing tasks")

            val existingTasks = taskDao.getAllTasksOnce()

            existingTasks.forEach { entity ->
                val task = TaskItem(
                    id = entity.id,
                    name = entity.name,
                    timestamp = entity.timestamp,
                    reminderMinutes = entity.reminderMinutes
                )

                NotificationScheduler.cancelTaskNotification(context, task)
                Log.d(TAG, "IMPORT cancelled existing main notification: id=${task.id}")

                if (task.reminderMinutes > 0) {
                    NotificationScheduler.cancelReminderNotification(context, task)
                    Log.d(TAG, "IMPORT cancelled existing before-reminder: id=${task.id}")
                }
            }

            taskDao.deleteAll()
            Log.d(TAG, "IMPORT deleted all existing Room tasks")
        }

        taskDao.insertTasks(importedTasks)
        Log.d(TAG, "IMPORT inserted tasks into Room: count=${importedTasks.size}")

        importedTasks.forEach { entity ->
            val task = TaskItem(
                id = entity.id,
                name = entity.name,
                timestamp = entity.timestamp,
                reminderMinutes = entity.reminderMinutes
            )

            NotificationScheduler.scheduleTaskNotification(context, task)
            Log.d(TAG, "IMPORT scheduled main notification: id=${task.id}")

            if (task.reminderMinutes > 0) {
                NotificationScheduler.scheduleReminderNotification(context, task)
                Log.d(
                    TAG,
                    "IMPORT scheduled before-reminder: id=${task.id}, minutes=${task.reminderMinutes}"
                )
            }
        }

        logAllTasks("after import")
    }

    private suspend fun logAllTasks(label: String) {
        val allTasks = taskDao.getAllTasksOnce()

        Log.d(TAG, "Task count $label: ${allTasks.size}")

        allTasks.forEach { entity ->
            Log.d(
                TAG,
                "Room task: id=${entity.id}, name=${entity.name}, " +
                        "timestamp=${entity.timestamp}, reminder=${entity.reminderMinutes}"
            )
        }
    }
}