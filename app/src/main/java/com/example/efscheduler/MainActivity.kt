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
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.efscheduler.models.notification.NotificationHelper
import com.example.efscheduler.ui.ExactAlarmPermissionDialog
import com.example.efscheduler.ui.SchedulerApp
import com.example.efscheduler.ui.theme.EFSchedulerTheme

class MainActivity : ComponentActivity() {

    private var showExactAlarmDialog by mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied. Notifications will not appear.")
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Log.d("MainActivity", "Notification permission already granted")
        }

        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) {
            Log.d("MainActivity", "Exact alarm permission already granted")
            showExactAlarmDialog = false
        } else {
            Log.d("MainActivity", "Exact alarm permission not granted. Prompting user.")
            showExactAlarmDialog = true
        }

        // Temporary workaround for Compose LazyColumn prefetch crash:
        // "Apply is called on deactivated node"
        ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = false

        setContent {
            EFSchedulerTheme {
                SchedulerApp()

                if (showExactAlarmDialog) {
                    ExactAlarmPermissionDialog(
                        onOpenSettings = {
                            showExactAlarmDialog = false

                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = "package:$packageName".toUri()
                            }

                            startActivity(intent)
                        },
                        onDismiss = {
                            showExactAlarmDialog = false
                        }
                    )
                }
            }
        }
    }
}