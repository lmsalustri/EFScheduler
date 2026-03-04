package com.example.efscheduler.models

import android.app.Application
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.efscheduler.models.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

class SchedulerViewModel(
    application: Application
) : ViewModel() {

    private val repository = TaskRepository(application)

    // Expose tasks as StateFlow (for Compose)
    private val _tasks = MutableStateFlow<List<TaskItem>?>(null)
    val tasks: StateFlow<List<TaskItem>?> = _tasks.asStateFlow()

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

    init {
        // Create notification channel
        NotificationHelper.createNotificationChannel(application)

        // Collect tasks from repository
        viewModelScope.launch {
            repository.tasksFlow.collect { taskList ->
                _tasks.value = taskList.sortedBy { it.timestamp }
            }
        }
    }

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
        taskToDelete?.let { task ->
            viewModelScope.launch {
                repository.deleteTask(task)
                taskToDelete = null
            }
        }
        if (_tasks.value?.isEmpty() == true) {  // safe call
            isListEditMode = false
        }
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
                viewModelScope.launch {
                    val newTask = TaskItem(
                        name = currentRun.taskName,
                        timestamp = timestamp
                    )
                    // If editing, pass the old task id so its notification is canceled
                    repository.addTask(newTask, editingTaskId)
                    editingTaskId = null
                }
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
            in 2..7 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
        return "$dayString at $timeString"
    }
}