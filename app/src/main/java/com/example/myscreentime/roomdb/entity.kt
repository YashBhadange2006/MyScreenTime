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

@Entity(tableName = "activity_data_table")
data class ActivityDataEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val walkingMs: Long = 0,
    val walkingUpstairsMs: Long = 0,
    val walkingDownstairsMs: Long = 0,
    val sittingMs: Long = 0,
    val standingMs: Long = 0,
    val layingMs: Long = 0
)
