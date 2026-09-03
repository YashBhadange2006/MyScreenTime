package com.example.myscreentime.fragments.wellbeingscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myscreentime.roomdb.ActivityDataEntity
import com.example.myscreentime.roomdb.AppRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityClassificationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var classifier: ActivityClassifier
    private var lastResultTime = System.currentTimeMillis()

    private val dailyAccumulator = mutableMapOf<String, Long>()
    private val accumulatorLock = Any()

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())

        classifier = ActivityClassifier(this) { label, _ ->
            _latestActivity.value = label
            val currentTime = System.currentTimeMillis()
            val duration = currentTime - lastResultTime
            lastResultTime = currentTime

            synchronized(accumulatorLock) {
                dailyAccumulator[label] = dailyAccumulator.getOrDefault(label, 0L) + duration
            }

            // Save to DB every minute or so (simplification: save every 30 results ~ 1.5 min)
            if (labelCount++ % 30 == 0) {
                saveAccumulatedData()
            }
        }
        classifier.start()
    }

    private var labelCount = 0

    private fun saveAccumulatedData() {
        val pendingData = synchronized(accumulatorLock) {
            dailyAccumulator.toMap().also { dailyAccumulator.clear() }
        }
        if (pendingData.isEmpty()) return

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        serviceScope.launch {
            persistActivityData(date, pendingData)
        }
    }

    private suspend fun persistActivityData(date: String, activityDurations: Map<String, Long>) {
        val dao = AppRoomDatabase.getInstance(applicationContext).usageDao()
        dao.ensureActivityData(ActivityDataEntity(date))
        dao.addActivityDurations(
            date = date,
            walkingMs = activityDurations.getOrDefault("Walking", 0L),
            walkingUpstairsMs = activityDurations.getOrDefault("Walking Upstairs", 0L),
            walkingDownstairsMs = activityDurations.getOrDefault("Walking Downstairs", 0L),
            sittingMs = activityDurations.getOrDefault("Sitting", 0L),
            standingMs = activityDurations.getOrDefault("Standing", 0L),
            layingMs = activityDurations.getOrDefault("Laying", 0L)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        classifier.close()
        val pendingData = synchronized(accumulatorLock) {
            dailyAccumulator.toMap().also { dailyAccumulator.clear() }
        }
        if (pendingData.isNotEmpty()) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            runBlocking(Dispatchers.IO) { persistActivityData(date, pendingData) }
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "activity_tracker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Activity Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Activity Tracker Running")
            .setContentText("Monitoring your physical activity in the background.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private val _latestActivity = MutableStateFlow("Waiting for activity data…")
        val latestActivity = _latestActivity.asStateFlow()
    }
}
