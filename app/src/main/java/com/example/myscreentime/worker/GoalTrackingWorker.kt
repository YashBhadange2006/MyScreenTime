package com.example.myscreentime.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myscreentime.fragments.permissionscreen.getTodayScreenTime
import com.example.myscreentime.utils.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoalTrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sharedPref = applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isEnabled = sharedPref.getBoolean("daily_goal_alert_enabled", false)

        Log.d("GoalTrackingWorker", "Worker triggered. Enabled: $isEnabled")

        if (!isEnabled) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("GoalTrackingWorker", "No notification permission")
                return Result.success()
            }
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastNotifiedDate = sharedPref.getString("last_goal_notified_date", "")

        if (today == lastNotifiedDate) {
            Log.d("GoalTrackingWorker", "Already notified today")
            return Result.success()
        }

        val todayTimeMs = getTodayScreenTime(applicationContext)
        val goalHours = sharedPref.getFloat("screen_time_goal", 4f)
        val goalMs = (goalHours * 60 * 60 * 1000).toLong()

        Log.d("GoalTrackingWorker", "Today: $todayTimeMs ms, Goal: $goalMs ms")

        if (todayTimeMs > goalMs) {
            val totalTimeStr = formatTime(todayTimeMs)
            val goalTimeStr = formatTime(goalMs)
            
            NotificationHelper.showGoalExceededNotification(
                applicationContext,
                totalTimeStr,
                goalTimeStr
            )

            sharedPref.edit().putString("last_goal_notified_date", today).apply()
            Log.d("GoalTrackingWorker", "Sent goal notification!")
        }

        return Result.success()
    }

    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }

    companion object {
        private const val WORK_NAME = "goal_tracking_worker"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<GoalTrackingWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun runOnce(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<GoalTrackingWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
