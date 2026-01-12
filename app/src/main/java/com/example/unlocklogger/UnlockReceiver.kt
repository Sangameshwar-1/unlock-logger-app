package com.example.unlocklogger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UnlockReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "UnlockReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "onReceive called!")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "========================================")
        
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "USER_PRESENT received - starting service")
                UnlockLoggerService.logUnlock(context)
            }
            "com.example.unlocklogger.TEST_UNLOCK" -> {
                Log.d(TAG, "TEST broadcast received - logging test unlock")
                UnlockLoggerService.logUnlock(context)
            }
            else -> {
                Log.w(TAG, "Received unexpected action: ${intent.action}")
            }
        }
    }
}
