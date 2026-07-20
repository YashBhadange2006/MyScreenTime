package com.example.myscreentime.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myscreentime.R

object NotificationHelper {
    private const val CHANNEL_ID = "screen_time_goal_channel"
    private const val CHANNEL_NAME = "Screen Time Alerts"
    private const val NOTIFICATION_ID = 1001

    fun showGoalExceededNotification(context: Context, totalTime: String, goalTime: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when daily screen time goal is exceeded"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_outline)
            .setContentTitle("Daily Goal Exceeded")
            .setContentText("You've spent $totalTime on your phone today. Your goal was $goalTime.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
