package com.example.efscheduler.models.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.efscheduler.models.TaskItem

object NotificationScheduler {
    private const val REMINDER_OFFSET = 1000000 // offset for reminder request codes

    fun scheduleTaskNotification(context: Context, task: TaskItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check exact alarm permission on Android 12+
        if (!alarmManager.canScheduleExactAlarms()
        ) {
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TASK_NAME", task.name)
            putExtra("TASK_ID", task.id)
            putExtra("IS_REMINDER", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(), // unique request code for main notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm at task timestamp
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            task.timestamp,
            pendingIntent
        )
    }

    fun cancelTaskNotification(context: Context, task: TaskItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun scheduleReminderNotification(context: Context, task: TaskItem) {
        if (task.reminderMinutes <= 0) return // no reminder

        val reminderTime = task.timestamp - (task.reminderMinutes * 60 * 1000L)
        if (reminderTime <= System.currentTimeMillis()) return // already passed

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()
        ) {
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TASK_NAME", task.name)
            putExtra("TASK_ID", task.id)
            putExtra("IS_REMINDER", true)
            putExtra("REMINDER_MINUTES", task.reminderMinutes)  // 👈 add this
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode() + REMINDER_OFFSET, // unique code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    fun cancelReminderNotification(context: Context, task: TaskItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode() + REMINDER_OFFSET,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}