package com.example.myscreentime.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_table", primaryKeys = ["packageName","date"])
data class AppUsageEntity(
    val packageName: String,
    val date: String,
    val totalTimeInForeground: Long
)

@Entity(tableName = "total_usage_table")
data class TotalUsageEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val totalCombinedTime: Long
)
