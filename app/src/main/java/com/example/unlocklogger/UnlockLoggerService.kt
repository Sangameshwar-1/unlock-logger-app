package com.example.unlocklogger

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.unlocklogger.data.UnlockEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UnlockLoggerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var powerManager: PowerManager
    private var wasScreenOff = false
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Screen broadcast received: ${intent.action}")
            
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen turned OFF")
                    wasScreenOff = true
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen turned ON, wasScreenOff=$wasScreenOff")
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "USER_PRESENT in service!")
                    if (wasScreenOff) {
                        Log.i(TAG, "Device was unlocked after screen off - logging event")
                        logUnlockEvent()
                        wasScreenOff = false
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "════════════════════════════════════")
        Log.e(TAG, "SERVICE CREATED!")
        Log.e(TAG, "════════════════════════════════════")
        
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(0))
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
        
        Log.d(TAG, "Service started and receiver registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        
        if (intent?.action == ACTION_LOG_UNLOCK) {
            Log.d(TAG, "Received ACTION_LOG_UNLOCK")
            logUnlockEvent()
        }
        
        return START_STICKY
    }

    private fun logUnlockEvent() {
        val timestamp = System.currentTimeMillis()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = formatter.format(Date(timestamp))
        
        Log.i(TAG, "╔════════════════════════════════════════════")
        Log.i(TAG, "║ UNLOCK EVENT DETECTED!")
        Log.i(TAG, "║ Time: $formattedTime")
        Log.i(TAG, "║ Timestamp: $timestamp")
        Log.i(TAG, "╚════════════════════════════════════════════")
        
        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val event = UnlockEvent(timestamp = timestamp)
                db.unlockEventDao().insert(event)
                
                val count = db.unlockEventDao().getUnlockCount()
                Log.i(TAG, "✅ Saved to database! Total unlocks: $count")
                
                val notification = createNotification(count)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving unlock event", e)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Unlock Logger",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks device unlock events"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(unlockCount: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Unlock Logger Active")
            .setContentText("Total unlocks: $unlockCount")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "════════════════════════════════════")
        Log.e(TAG, "SERVICE DESTROYED!")
        Log.e(TAG, "════════════════════════════════════")
        
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "UnlockLoggerService"
        private const val CHANNEL_ID = "unlock_logger_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_LOG_UNLOCK = "com.example.unlocklogger.LOG_UNLOCK"
        
        fun logUnlock(context: Context) {
            Log.d(TAG, "logUnlock() called from external source")
            val intent = Intent(context, UnlockLoggerService::class.java).apply {
                action = ACTION_LOG_UNLOCK
            }
            context.startForegroundService(intent)
        }
    }
}
