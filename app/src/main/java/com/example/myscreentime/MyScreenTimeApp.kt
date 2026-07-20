package com.example.myscreentime

import android.app.Application
import com.example.myscreentime.worker.DailyUsageSyncWorker

class MyScreenTimeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DailyUsageSyncWorker.schedule(this)
    }
}
