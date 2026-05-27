package com.example.efscheduler.models.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.efscheduler.models.TaskDatabase
import com.example.efscheduler.models.TaskItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != ACTION_QUICKBOOT_POWERON
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Boot broadcast received: ${intent.action}")

                val database = TaskDatabase.getInstance(context)
                val taskDao = database.taskDao()

                val now = System.currentTimeMillis()

                val tasks = taskDao.getAllTasksOnce()
                    .filter { entity -> entity.timestamp > now }
                    .map { entity ->
                        TaskItem(
                            id = entity.id,
                            name = entity.name,
                            timestamp = entity.timestamp,
                            reminderMinutes = entity.reminderMinutes
                        )
                    }

                Log.d(TAG, "Future tasks found after reboot: ${tasks.size}")

                tasks.forEach { task ->
                    NotificationScheduler.scheduleTaskNotification(context, task)

                    if (task.reminderMinutes > 0) {
                        NotificationScheduler.scheduleReminderNotification(context, task)
                    }

                    Log.d(
                        TAG,
                        "Rescheduled task=${task.name}, timestamp=${task.timestamp}, reminder=${task.reminderMinutes}"
                    )
                }

                Log.d(TAG, "Boot reschedule complete")
            } catch (e: Exception) {
                Log.e(TAG, "Boot reschedule failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "EFSchedulerBoot"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}