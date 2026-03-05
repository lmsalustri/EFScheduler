package com.example.efscheduler.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val timestamp: Long,
    val reminderMinutes: Int = 0
)