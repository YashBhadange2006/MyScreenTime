package com.example.myscreentime.fragments.dashboardscreen

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myscreentime.fragments.permissionscreen.*
import com.example.myscreentime.roomdb.AppRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class DashboardData(
    val totalTime: Long,
    val breakdownProportions: List<Float>, // Ratios of top apps to total time
    val breakdownLabels: List<String>,     // Names of top apps
    val breakdownTimes: List<Long>,       // Actual times for top apps
    val mostUsedPackage: String?,
    val mostUsedName: String?,
    val lastUsedPackage: String?,
    val lastUsedName: String?,
    val usageItems: List<RowItem>,
    val percentText: String,
    val isMoreThanYesterday: Boolean
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<DashboardData?>(null)
    val uiState: StateFlow<DashboardData?> = _uiState.asStateFlow()

    fun loadDataIfNeeded() {
        if (_uiState.value == null) {
            refreshData()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                // 1. Sync current real-time data to Room first
                syncTodayDataToRoom()
                // 2. Build UI data from Room DB source
                buildDashboardDataFromRoom()
            }
            _uiState.value = data
        }
    }

    private suspend fun syncTodayDataToRoom() {
        val context = getApplication<Application>().applicationContext
        val database = AppRoomDatabase.getInstance(context)
        val dao = database.usageDao()

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        val usageEntries = getSortedUsedApps(context) // Proactively filtered list

        val appEntities = usageEntries.map {
            com.example.myscreentime.roomdb.AppUsageEntity(
                packageName = it.packageName,
                date = todayDate,
                totalTimeInForeground = it.totalTimeInForeground,
                lastTimeUsed = it.lastTimeUsed
            )
        }

        val totalTime = getTodayScreenTime(context)
        val totalEntity = com.example.myscreentime.roomdb.TotalUsageEntity(
            date = todayDate,
            totalCombinedTime = totalTime
        )

        dao.insertUsageData(appEntities)
        dao.insertTotalData(totalEntity)
    }

    private suspend fun buildDashboardDataFromRoom(): DashboardData {
        val context = getApplication<Application>().applicationContext
        val database = AppRoomDatabase.getInstance(context)
        val dao = database.usageDao()
        
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        
        // Fetch from ROOM
        val todayTotal = dao.getTotalUsageForDate(todayDate)?.totalCombinedTime ?: 0L
        val todayApps = dao.getUsageRowsForDate(todayDate)

        // Comparison with yesterday (from ROOM)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val yesterdayTotal = dao.getTotalUsageForDate(yesterdayDate)?.totalCombinedTime ?: 0L
        
        var percentCompText = "0.0%"
        var isMoreThanYesterday = false
        
        if (yesterdayTotal > 0) {
            val diff = todayTotal - yesterdayTotal
            val percent = (abs(diff).toDouble() / yesterdayTotal.toDouble()) * 100
            isMoreThanYesterday = diff > 0
            percentCompText = String.format(Locale.getDefault(), "%.1f%%", percent)
        }

        // Group into Top 4 + Other
        val top4 = todayApps.take(4)
        val others = todayApps.drop(4)
        val othersTime = others.sumOf { it.totalTimeInForeground }

        val breakdownLabels = mutableListOf<String>()
        val breakdownProportions = mutableListOf<Float>()
        val breakdownTimes = mutableListOf<Long>()

        top4.forEach { 
            breakdownLabels.add(resolveAppName(it.packageName))
            breakdownProportions.add(if (todayTotal > 0) it.totalTimeInForeground.toFloat() / todayTotal.toFloat() else 0f)
            breakdownTimes.add(it.totalTimeInForeground)
        }

        if (othersTime > 0) {
            breakdownLabels.add("Other")
            breakdownProportions.add(if (todayTotal > 0) othersTime.toFloat() / todayTotal.toFloat() else 0f)
            breakdownTimes.add(othersTime)
        }

        // Build list for the recycler view below (also from Room data)
        val usageItems = todayApps.map { entry ->
            RowItem(
                packageName = entry.packageName,
                appName = resolveAppName(entry.packageName),
                usageTime = formatTime(entry.totalTimeInForeground),
                progress = if (todayTotal > 0) entry.totalTimeInForeground.toFloat() / todayTotal.toFloat() else 0f
            )
        }

        val mostUsed = todayApps.firstOrNull()
        val lastUsed = todayApps.maxByOrNull { it.lastTimeUsed }

        return DashboardData(
            totalTime = todayTotal,
            breakdownProportions = breakdownProportions,
            breakdownLabels = breakdownLabels,
            breakdownTimes = breakdownTimes,
            mostUsedPackage = mostUsed?.packageName,
            mostUsedName = mostUsed?.packageName?.let { resolveAppName(it) },
            lastUsedPackage = lastUsed?.packageName,
            lastUsedName = lastUsed?.packageName?.let { resolveAppName(it) },
            usageItems = usageItems,
            percentText = percentCompText,
            isMoreThanYesterday = isMoreThanYesterday
        )
    }

    private fun resolveAppName(packageName: String): String {
        val context = getApplication<Application>().applicationContext
        val pm = context.packageManager
        
        // Strip process suffix (e.g., :remote)
        val cleanPackageName = packageName.substringBefore(':')
        
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(cleanPackageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pm.getLaunchIntentForPackage(cleanPackageName)
                ?.resolveActivityInfo(pm, PackageManager.MATCH_DEFAULT_ONLY)
                ?.loadLabel(pm)
                ?.toString()
                ?: run {
                    val parts = cleanPackageName.split('.')
                    val candidate = parts.lastOrNull { it !in setOf("android", "google", "apps", "main", "core") }
                        ?: parts.lastOrNull()
                        ?: cleanPackageName
                    candidate.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                }
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }
}
