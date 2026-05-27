package com.example.efscheduler.models.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.example.efscheduler.models.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("TASK_NAME") ?: run {
            Log.w(TAG, "NotificationReceiver fired but TASK_NAME was missing")
            return
        }

        val taskId = intent.getStringExtra("TASK_ID") ?: run {
            Log.w(TAG, "NotificationReceiver fired but TASK_ID was missing")
            return
        }

        val isReminder = intent.getBooleanExtra("IS_REMINDER", false)
        val reminderMinutes = intent.getIntExtra("REMINDER_MINUTES", 0)

        Log.d(
            TAG,
            "NotificationReceiver fired: taskName=$taskName, taskId=$taskId, isReminder=$isReminder, reminderMinutes=$reminderMinutes"
        )

        NotificationHelper.createNotificationChannel(context)

        val (title, text) = if (isReminder) {
            Pair(
                "Reminder: Task starting soon",
                "\"$taskName\" starts in ${formatMinutes(reminderMinutes)}."
            )
        } else {
            Pair(
                "Time to start your task!",
                "\"$taskName\" is scheduled now."
            )
        }

        val notification = NotificationHelper.buildNotification(context, title, text)
        val notificationId = taskId.hashCode() + if (isReminder) 1 else 0

        Log.d(
            TAG,
            "Posting notification: notificationId=$notificationId, title=$title"
        )

        NotificationManagerCompat.from(context).notify(notificationId, notification)

        if (!isReminder) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = TaskDatabase.getInstance(context)
                    val taskDao = database.taskDao()

                    taskDao.deleteTaskById(taskId)

                    Log.d(
                        TAG,
                        "Final task notification fired. Deleted task from Room: taskId=$taskId, taskName=$taskName"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete completed task after final notification", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun formatMinutes(minutes: Int): String {
        return when (minutes) {
            60 -> "1 hour"
            120 -> "2 hours"
            else -> "$minutes minutes"
        }
    }

    companion object {
        private const val TAG = "EFSchedulerNotif"
    }
}