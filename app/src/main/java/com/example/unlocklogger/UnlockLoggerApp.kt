package com.example.unlocklogger

import android.app.Application
import android.util.Log

class UnlockLoggerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.e("UnlockLoggerApp", "════════════════════════════════════")
        Log.e("UnlockLoggerApp", "APPLICATION STARTED!")
        Log.e("UnlockLoggerApp", "Process ID: ${android.os.Process.myPid()}")
        Log.e("UnlockLoggerApp", "════════════════════════════════════")
    }
}
