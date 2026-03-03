// NotificationReceiver.kt
package com.example.efscheduler.models.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    @RequiresPermission("android.permission.POST_NOTIFICATIONS")
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("TASK_NAME") ?: return
        val taskId = intent.getStringExtra("TASK_ID") ?: return

        // Ensure notification channel exists
        NotificationHelper.createNotificationChannel(context)

        val notification = NotificationHelper.buildNotification(context, taskName)

        with(NotificationManagerCompat.from(context)) {
            notify(taskId.hashCode(), notification) // use taskId as unique notification id
        }
    }
}