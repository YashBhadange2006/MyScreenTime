package com.example.myscreentime

import android.app.Application
import com.example.myscreentime.worker.DailyUsageSyncWorker
import com.example.myscreentime.worker.GoalTrackingWorker

class MyScreenTimeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DailyUsageSyncWorker.schedule(this)
        GoalTrackingWorker.schedule(this)
    }
}
