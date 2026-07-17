package com.example.myscreentime.fragments.permissionscreen

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.RequiresApi
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.Q)
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

@RequiresApi(Build.VERSION_CODES.Q)
fun requestUsageStatsPermission(context: Context) {
    if (!hasUsageStatsPermission(context)) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }
}

fun getAppUsageStats(context: Context) {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val calendar = Calendar.getInstance()
    // Set to the start of the current day (midnight)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    for(usageStat in stats){
        val packageName = usageStat.packageName
        val totalTimeInForeground = usageStat.totalTimeInForeground
        println("App: $packageName, Duration: ${totalTimeInForeground/1000} seconds")
    }
}

fun getTodayScreenTime(context: Context): Long {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    var totalScreenTime = 0L

    for (usageStat in stats) {
        totalScreenTime += usageStat.totalTimeInForeground
    }

    return totalScreenTime
}

fun getMostUsedApp(context: Context): UsageStats? {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        calendar.timeInMillis,
        System.currentTimeMillis()
    )

    return stats
        .filter { it.packageName != context.packageName }
        .maxByOrNull { it.totalTimeInForeground }
}

fun getLastUsedApp(context: Context): UsageStats? {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        System.currentTimeMillis() - 24 * 60 * 60 * 1000,
        System.currentTimeMillis()
    )

    return stats
        .filter { it.packageName != context.packageName }
        .maxByOrNull { it.lastTimeUsed }
}

