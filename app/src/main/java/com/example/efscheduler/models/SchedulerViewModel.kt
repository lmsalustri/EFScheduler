package com.example.efscheduler.models

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class Run(
    val taskName: String = "",
    val startTimestamp: Long? = null
)

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val timestamp: Long
)

class SchedulerViewModel : ViewModel() {

    private val _tasks = mutableStateListOf<TaskItem>()

    val tasks: List<TaskItem>
        get() = _tasks.sortedBy { it.timestamp }

    var isEditing by mutableStateOf(false)
        private set

    var currentRun by mutableStateOf(Run())
        private set

    var taskNameInput by mutableStateOf("")
        private set

    var selectedTimestamp by mutableStateOf<Long?>(null)
        private set

    var isListEditMode by mutableStateOf(false)
        private set

    var taskToDelete: TaskItem? by mutableStateOf(null)
        private set

    private var editingTaskId: String? by mutableStateOf(null)

    val isNextEnabled: Boolean
        get() = taskNameInput.trim().length >= 2

    fun toggleListEditMode() {
        isListEditMode = !isListEditMode
    }

    fun startEditingTask(task: TaskItem) {
        editingTaskId = task.id
        startEditing(task.name)
        selectedTimestamp = task.timestamp
        isListEditMode = false
    }

    fun confirmDeleteTask(task: TaskItem) {
        taskToDelete = task
    }

    fun deleteConfirmed() {
        taskToDelete?.let { _tasks.remove(it) }
        taskToDelete = null
    }

    fun cancelDelete() {
        taskToDelete = null
    }

    fun startEditing(taskName: String) {
        isEditing = true
        taskNameInput = taskName
        currentRun = currentRun.copy(taskName = taskName)
    }

    fun startNewTask() {
        isEditing = false
        taskNameInput = ""
        selectedTimestamp = null
        currentRun = Run()
        isListEditMode = false
        editingTaskId = null
    }

    fun updateTaskNameInput(newText: String) {
        taskNameInput = newText
    }

    fun confirmTaskName() {
        currentRun = currentRun.copy(taskName = taskNameInput.trim())
    }

    fun updateSelectedTimestamp(timestamp: Long) {
        selectedTimestamp = timestamp
    }

    fun confirmDateTime() {
        selectedTimestamp?.let {
            currentRun = currentRun.copy(startTimestamp = it)
        }
    }

    fun addCurrentRunToTasks() {
        currentRun.startTimestamp?.let { timestamp ->
            if (currentRun.taskName.isNotBlank()) {
                editingTaskId?.let { id ->
                    _tasks.removeAll { it.id == id }
                    editingTaskId = null
                }
                _tasks.add(
                    TaskItem(
                        name = currentRun.taskName,
                        timestamp = timestamp
                    )
                )
            }
        }
    }

    fun finishRun() {
        currentRun = Run()
        isEditing = false
        taskNameInput = ""
        selectedTimestamp = null
        editingTaskId = null
    }

    fun cancelEditing() {
        if (isEditing) {
            finishRun()
        } else {
            taskNameInput = ""
        }
        editingTaskId = null
    }

    /**
     * Formats a timestamp into a user-friendly string based on how far away it is.
     * Examples: "Today at 3:30 PM", "Tomorrow at 10:00 AM", "Monday at 2:00 PM", "Jan 1 at 9:00 AM"
     */
    fun formatDateTime(context: Context, timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetDay = calendar.clone() as Calendar
        targetDay.set(Calendar.HOUR_OF_DAY, 0)
        targetDay.set(Calendar.MINUTE, 0)
        targetDay.set(Calendar.SECOND, 0)
        targetDay.set(Calendar.MILLISECOND, 0)

        val daysDifference = TimeUnit.MILLISECONDS.toDays(targetDay.timeInMillis - today.timeInMillis).toInt()

        val timeFormat = if (DateFormat.is24HourFormat(context)) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
        val timeString = timeFormat.format(Date(timestamp))

        val dayString = when (daysDifference) {
            0 -> "Today"
            1 -> "Tomorrow"
            in 2..7 -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
        return "$dayString at $timeString"
    }
}