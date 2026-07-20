package com.example.myscreentime.fragments.permissionscreen

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.RequiresApi
import java.util.Calendar

data class AppUsageEntry(
    val packageName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long
)

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
    val startTime = getStartOfDayMillis()
    val endTime = System.currentTimeMillis()
    return getTotalScreenTimeForRange(context, startTime, endTime)
}

fun getMostUsedApp(context: Context): AppUsageEntry? {
    return getRelevantUsageEntries(context)
        .maxByOrNull { it.totalTimeInForeground }
}

fun getLastUsedApp(context: Context): AppUsageEntry? {
    return getRelevantUsageEntries(context)
        .maxByOrNull { it.lastTimeUsed }
}

fun getSortedUsedApps(context: Context): List<AppUsageEntry> {
    return getRelevantUsageEntries(context).sortedByDescending {
        it.totalTimeInForeground
    }
}

fun getUsageEntriesForRange(
    context: Context,
    startTime: Long,
    endTime: Long
): List<AppUsageEntry> {
    return getRelevantUsageEntries(context, startTime, endTime)
}

fun getTotalScreenTimeForRange(
    context: Context,
    startTime: Long,
    endTime: Long
): Long {
    val elapsedWallClock = (endTime - startTime).coerceAtLeast(0L)
    val total = getRelevantUsageEntries(context, startTime, endTime)
        .sumOf { it.totalTimeInForeground }

    return total.coerceAtMost(elapsedWallClock)
}

private fun getRelevantUsageEntries(context: Context): List<AppUsageEntry> {
    return getRelevantUsageEntries(context, getStartOfDayMillis(), System.currentTimeMillis())
}

private fun getRelevantUsageEntries(
    context: Context,
    startTime: Long,
    endTime: Long
): List<AppUsageEntry> {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()
    val totalForegroundTime = mutableMapOf<String, Long>()
    val lastUsedTimes = mutableMapOf<String, Long>()
    var currentForegroundPackage: String? = null
    var currentForegroundStart: Long? = null
    var isScreenInteractive = true

    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)

        when (event.eventType) {
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                closeForegroundSession(
                    currentForegroundPackage,
                    currentForegroundStart,
                    event.timeStamp,
                    totalForegroundTime
                )
                currentForegroundPackage = null
                currentForegroundStart = null
                isScreenInteractive = false
            }

            UsageEvents.Event.SCREEN_INTERACTIVE -> {
                isScreenInteractive = true
            }

            UsageEvents.Event.ACTIVITY_RESUMED,
            UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                val packageName = event.packageName ?: continue
                if (!isScreenInteractive) {
                    continue
                }

                closeForegroundSession(
                    currentForegroundPackage,
                    currentForegroundStart,
                    event.timeStamp,
                    totalForegroundTime
                )

                currentForegroundPackage = packageName
                currentForegroundStart = event.timeStamp
                lastUsedTimes[packageName] = maxOf(lastUsedTimes[packageName] ?: 0L, event.timeStamp)
            }

            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                val packageName = event.packageName ?: continue
                lastUsedTimes[packageName] = maxOf(lastUsedTimes[packageName] ?: 0L, event.timeStamp)

                if (packageName == currentForegroundPackage) {
                    closeForegroundSession(
                        currentForegroundPackage,
                        currentForegroundStart,
                        event.timeStamp,
                        totalForegroundTime
                    )
                    currentForegroundPackage = null
                    currentForegroundStart = null
                }
            }
        }
    }

    if (isScreenInteractive) {
        closeForegroundSession(
            currentForegroundPackage,
            currentForegroundStart,
            endTime,
            totalForegroundTime
        )
    }

    return mergeWithUsageStatsFallback(
        context = context,
        totalForegroundTime = totalForegroundTime,
        lastUsedTimes = lastUsedTimes,
        usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
    )
}

private fun getStartOfDayMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun closeForegroundSession(
    packageName: String?,
    startTime: Long?,
    endTime: Long,
    totalForegroundTime: MutableMap<String, Long>
) {
    if (packageName == null || startTime == null) {
        return
    }

    val duration = (endTime - startTime).coerceAtLeast(0L)
    if (duration > 0L) {
        totalForegroundTime[packageName] = (totalForegroundTime[packageName] ?: 0L) + duration
    }
}

private fun mergeWithUsageStatsFallback(
    context: Context,
    totalForegroundTime: Map<String, Long>,
    lastUsedTimes: Map<String, Long>,
    usageStats: List<UsageStats>
): List<AppUsageEntry> {
    val mergedEntries = linkedMapOf<String, AppUsageEntry>()
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val homePackage = context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName

    totalForegroundTime.forEach { (packageName, totalTime) ->
        if (totalTime > 0L && !shouldExcludeDashboardPackage(context, packageName, homePackage)) {
            mergedEntries[packageName] = AppUsageEntry(
                packageName = packageName,
                totalTimeInForeground = totalTime,
                lastTimeUsed = lastUsedTimes[packageName] ?: 0L
            )
        }
    }

    usageStats.forEach { stats ->
        if (
            stats.totalTimeInForeground <= 0L ||
            shouldExcludeDashboardPackage(context, stats.packageName, homePackage)
        ) {
            return@forEach
        }

        val existingEntry = mergedEntries[stats.packageName]
        if (existingEntry == null) {
            mergedEntries[stats.packageName] = AppUsageEntry(
                packageName = stats.packageName,
                totalTimeInForeground = stats.totalTimeInForeground,
                lastTimeUsed = stats.lastTimeUsed
            )
        } else if (stats.lastTimeUsed > existingEntry.lastTimeUsed) {
            mergedEntries[stats.packageName] = existingEntry.copy(
                lastTimeUsed = stats.lastTimeUsed
            )
        }
    }

    return mergedEntries.values.toList()
}

private fun shouldExcludeDashboardPackage(
    context: Context,
    packageName: String,
    homePackage: String?
): Boolean {
    if (packageName == homePackage) {
        return true
    }

    val appLabel = try {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(packageName, 0)
        ).toString().trim().lowercase()
    } catch (_: Exception) {
        packageName.substringAfterLast('.').trim().lowercase()
    }

    return appLabel in setOf("android", "gm", "system ui", "one ui home")
}

