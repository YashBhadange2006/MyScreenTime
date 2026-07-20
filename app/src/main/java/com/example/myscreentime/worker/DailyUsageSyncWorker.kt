package com.example.myscreentime.worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myscreentime.fragments.permissionscreen.hasUsageStatsPermission
import com.example.myscreentime.fragments.permissionscreen.getTotalScreenTimeForRange
import com.example.myscreentime.fragments.permissionscreen.getUsageEntriesForRange
import com.example.myscreentime.roomdb.AppRoomDatabase
import com.example.myscreentime.roomdb.AppUsageEntity
import com.example.myscreentime.roomdb.TotalUsageEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyUsageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        runSync(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_usage_sync"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<DailyUsageSyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        private fun calculateInitialDelay(): Long {
            val nextRun = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            return (nextRun.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        }

        private fun getPreviousDayWindow(): DayWindow {
            val startCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCalendar = startCalendar.clone() as Calendar
            endCalendar.add(Calendar.DAY_OF_YEAR, 1)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            return DayWindow(
                startTime = startCalendar.timeInMillis,
                endTime = endCalendar.timeInMillis,
                date = dateFormat.format(Date(startCalendar.timeInMillis))
            )
        }

        private fun getCutoffDate(daysToKeep: Int): String {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysToKeep)
            }
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private suspend fun runSync(context: Context) {
            withContext(Dispatchers.IO) {
                if (!hasUsageStatsPermission(context)) {
                    return@withContext
                }

                val database = AppRoomDatabase.getInstance(context)
                val dao = database.usageDao()
                val dayWindow = getPreviousDayWindow()

                val appUsageEntries = getUsageEntriesForRange(
                    context = context,
                    startTime = dayWindow.startTime,
                    endTime = dayWindow.endTime
                )

                val appEntities = appUsageEntries
                    .filter { it.totalTimeInForeground > 0L }
                    .map {
                        AppUsageEntity(
                            packageName = it.packageName,
                            date = dayWindow.date,
                            totalTimeInForeground = it.totalTimeInForeground
                        )
                    }

                val totalEntity = TotalUsageEntity(
                    date = dayWindow.date,
                    totalCombinedTime = getTotalScreenTimeForRange(
                        context = context,
                        startTime = dayWindow.startTime,
                        endTime = dayWindow.endTime
                    )
                )

                dao.insertUsageData(appEntities)
                dao.insertTotalData(totalEntity)

                val cutoffDate = getCutoffDate(daysToKeep = 30)
                dao.deleteOldAppUsage(cutoffDate)
                dao.deleteOldTotalUsage(cutoffDate)
            }
        }
    }
}

private data class DayWindow(
    val startTime: Long,
    val endTime: Long,
    val date: String
)
