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
                buildDashboardData()
            }
            _uiState.value = data
        }
    }

    private suspend fun buildDashboardData(): DashboardData {
        val context = getApplication<Application>().applicationContext
        val totalTime = getTodayScreenTime(context)
        val mostUsed = getMostUsedApp(context)
        val lastUsed = getLastUsedApp(context)

        // Calculate comparison with yesterday
        val db = AppRoomDatabase.getInstance(context)
        val dao = db.usageDao()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDate = dateFormat.format(calendar.time)

        val yesterdayTotal = dao.getTotalUsageForDate(yesterdayDate)?.totalCombinedTime ?: 0L
        
        var percentCompText = "No data from yesterday"
        var isMoreThanYesterday = false
        
        if (yesterdayTotal > 0) {
            val diff = totalTime - yesterdayTotal
            val percent = (abs(diff).toDouble() / yesterdayTotal.toDouble()) * 100
            isMoreThanYesterday = diff > 0
            val direction = if (isMoreThanYesterday) "more" else "less"
            percentCompText = "${String.format(Locale.getDefault(), "%.1f", percent)}% $direction than yesterday"
        }

        // Calculate breakdown proportions for the top 5 apps
        val sortedApps = getSortedUsedApps(context)
        val topApps = sortedApps.take(5)
        val breakdownProportions = topApps.map { 
            if (totalTime > 0) it.totalTimeInForeground.toFloat() / totalTime.toFloat() else 0f
        }
        val breakdownLabels = topApps.map { resolveAppName(it.packageName) }

        return DashboardData(
            totalTime = totalTime,
            breakdownProportions = breakdownProportions,
            breakdownLabels = breakdownLabels,
            mostUsedPackage = mostUsed?.packageName,
            mostUsedName = mostUsed?.packageName?.let { resolveAppName(it) },
            lastUsedPackage = lastUsed?.packageName,
            lastUsedName = lastUsed?.packageName?.let { resolveAppName(it) },
            usageItems = buildUsageItems(context, totalTime),
            percentText = percentCompText,
            isMoreThanYesterday = isMoreThanYesterday
        )
    }

    private fun buildUsageItems(context: android.content.Context, totalTime: Long): List<RowItem> {
        return getSortedUsedApps(context).map { usageEntry ->
            val appProgress = if (totalTime > 0) usageEntry.totalTimeInForeground.toFloat() / totalTime.toFloat() else 0f
            RowItem(
                packageName = usageEntry.packageName,
                appName = resolveAppName(usageEntry.packageName),
                usageTime = formatTime(usageEntry.totalTimeInForeground),
                progress = appProgress.coerceAtMost(1.0f)
            )
        }
    }

    private fun resolveAppName(packageName: String): String {
        val context = getApplication<Application>().applicationContext
        val pm = context.packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pm.getLaunchIntentForPackage(packageName)
                ?.resolveActivityInfo(pm, PackageManager.MATCH_DEFAULT_ONLY)
                ?.loadLabel(pm)
                ?.toString()
                ?: packageName.substringAfterLast('.').replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }
}
