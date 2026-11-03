package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeMillis: Long,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String = "", // Comma-separated day indices, e.g., "1,2,3,4,5" or empty for one-shot
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val isBengaliAnnouncement: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
