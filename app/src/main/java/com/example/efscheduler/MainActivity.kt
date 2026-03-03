package com.example.efscheduler

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.efscheduler.models.notification.NotificationHelper
import com.example.efscheduler.ui.SchedulerApp
import com.example.efscheduler.ui.theme.EFSchedulerTheme

class MainActivity : ComponentActivity() {

    // Launcher for notification permission (Android 13+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied – notifications will not appear")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel as early as possible
        NotificationHelper.createNotificationChannel(this)

        // Request notification permission on Android 13+
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Log.d("MainActivity", "Notification permission already granted")
        }

        // Check and request exact alarm permission on Android 12+
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            // Open system settings for the user to grant exact alarm permission
            Intent().apply {
                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                data = "package:$packageName".toUri()
                startActivity(this)
            }
        } else {
            Log.d("MainActivity", "Exact alarm permission already granted")
        }

        setContent {
            EFSchedulerTheme {
                SchedulerApp()
            }
        }
    }
}