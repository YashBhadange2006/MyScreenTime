package com.example.myscreentime.fragments.wellbeingscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myscreentime.roomdb.AppRoomDatabase
import com.example.myscreentime.roomdb.ActivityDataEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class WellBeingViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppRoomDatabase.getInstance(application).usageDao()
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val weekStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
    private val weekStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(weekStart.time)

    val uiState: StateFlow<WellBeingUiState> = combine(
        ActivityClassificationService.latestActivity,
        dao.observeActivityDataForDate(today),
        dao.observeActivityDataFrom(weekStartDate)
    ) { liveActivity, activityData, weeklyRows ->
        val todayData = activityData ?: ActivityDataEntity(today)
        WellBeingUiState.Success(
            liveActivity = liveActivity,
            walkingMs = todayData.walkingMs,
            walkingUpstairsMs = todayData.walkingUpstairsMs,
            walkingDownstairsMs = todayData.walkingDownstairsMs,
            sittingMs = todayData.sittingMs,
            standingMs = todayData.standingMs,
            layingMs = todayData.layingMs,
            weeklyActivity = buildWeeklyActivity(weeklyRows)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WellBeingUiState.Success(
            liveActivity = "Waiting for activity data…",
            walkingMs = 0, walkingUpstairsMs = 0, walkingDownstairsMs = 0,
            sittingMs = 0, standingMs = 0, layingMs = 0, weeklyActivity = emptyList()
        )
    )

    private fun buildWeeklyActivity(rows: List<ActivityDataEntity>): List<WeeklyActivity> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dataByDate = rows.associateBy { it.date }
        return (0..6).map { dayOffset ->
            val calendar = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            val row = dataByDate[dateFormat.format(calendar.time)]
            WeeklyActivity(
                label = labelFormat.format(calendar.time),
                walkingMs = row?.walkingMs ?: 0,
                walkingUpstairsMs = row?.walkingUpstairsMs ?: 0,
                walkingDownstairsMs = row?.walkingDownstairsMs ?: 0,
                sittingMs = row?.sittingMs ?: 0,
                standingMs = row?.standingMs ?: 0,
                layingMs = row?.layingMs ?: 0
            )
        }
    }
}
