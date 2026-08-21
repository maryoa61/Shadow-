package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_logs")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeFormatted: String,
    val level: String, // INFO, OK, DBG, ERR, CRIT
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
