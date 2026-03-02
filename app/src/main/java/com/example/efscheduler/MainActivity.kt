package com.example.efscheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.efscheduler.ui.SchedulerApp
import com.example.efscheduler.ui.theme.EFSchedulerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EFSchedulerTheme {
                SchedulerApp()
            }
        }
    }
}