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

    val uiState: StateFlow<WellBeingUiState> = combine(
        ActivityClassificationService.latestActivity,
        // Using a flow that emits the current date to trigger updates
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                kotlinx.coroutines.delay(60000) // Check every minute
            }
        },
        // Observe all data since 6 days ago
        dao.observeActivityDataFrom(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.time
            )
        )
    ) { liveActivity, todayDate, weeklyRows ->
        val todayData = weeklyRows.find { it.date == todayDate } ?: ActivityDataEntity(todayDate)
        
        WellBeingUiState.Success(
            liveActivity = liveActivity,
            walkingMs = todayData.walkingMs,
            walkingUpstairsMs = todayData.walkingUpstairsMs,
            walkingDownstairsMs = todayData.walkingDownstairsMs,
            sittingMs = todayData.sittingMs,
            standingMs = todayData.standingMs,
            layingMs = todayData.layingMs,
            weeklyActivity = buildWeeklyActivity(todayDate, weeklyRows)
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

    private fun buildWeeklyActivity(todayDate: String, rows: List<ActivityDataEntity>): List<WeeklyActivity> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dataByDate = rows.associateBy { it.date }
        
        val calendar = Calendar.getInstance().apply { 
            time = dateFormat.parse(todayDate) ?: Date()
            add(Calendar.DAY_OF_YEAR, -6) 
        }
        
        return (0..6).map { _ ->
            val dateStr = dateFormat.format(calendar.time)
            val row = dataByDate[dateStr]
            val activity = WeeklyActivity(
                label = labelFormat.format(calendar.time),
                walkingMs = row?.walkingMs ?: 0,
                walkingUpstairsMs = row?.walkingUpstairsMs ?: 0,
                walkingDownstairsMs = row?.walkingDownstairsMs ?: 0,
                sittingMs = row?.sittingMs ?: 0,
                standingMs = row?.standingMs ?: 0,
                layingMs = row?.layingMs ?: 0
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            activity
        }
    }
}
