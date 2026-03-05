package com.example.efscheduler.models.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("TASK_NAME") ?: return
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val isReminder = intent.getBooleanExtra("IS_REMINDER", false)
        val reminderMinutes = intent.getIntExtra("REMINDER_MINUTES", 0)

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
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun formatMinutes(minutes: Int): String {
        return when (minutes) {
            60 -> "1 hour"
            120 -> "2 hours"
            else -> "$minutes minutes"
        }
    }
}