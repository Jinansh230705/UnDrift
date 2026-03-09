package com.undrift.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.undrift.MainActivity
import com.undrift.data.MongoRepository
import com.undrift.data.UserPreferences
import com.undrift.utils.UsageStatsHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*

class FocusService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var userPreferences: UserPreferences
    private val mongoRepository = MongoRepository()
    
    private var isFocusModeActive = false
    private var focusEndTime = 0L
    private var monitoringJob: Job? = null
    
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0

    companion object {
        private const val TAG = "FocusService"
    }

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
        createNotificationChannel()
        startBackgroundMonitoring()
    }

    private fun startBackgroundMonitoring() {
        serviceScope.launch {
            while (true) {
                checkForegroundAndBlock()
                delay(800) 
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "START_FOCUS" -> {
                val durationSeconds = intent.getIntExtra("DURATION", 3600)
                startFocusMode(durationSeconds)
            }
            "STOP_FOCUS" -> stopFocusMode()
        }
        return START_STICKY
    }

    private fun startFocusMode(durationSeconds: Int) {
        isFocusModeActive = true
        focusEndTime = System.currentTimeMillis() + (durationSeconds * 1000L)
        
        val notification = createNotification("Focus Mode Active", "Stay away from distractions!")
        startForeground(1, notification)
        
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isFocusModeActive) {
                if (System.currentTimeMillis() >= focusEndTime) {
                    completeFocusSession()
                    break
                }
                delay(2000)
            }
        }
    }

    private fun stopFocusMode() {
        isFocusModeActive = false
        monitoringJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private suspend fun completeFocusSession() {
        val profile = userPreferences.userProfileFlow.first()
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        
        if (profile.lastStreakDay != currentDay) {
            val newPoints = profile.points + 300
            val newStreak = profile.streakCount + 1
            
            val history = profile.streakHistory.toMutableList()
            if (history.isNotEmpty()) {
                history[history.size - 1] += profile.focusDurationMinutes / 60
            }

            userPreferences.updatePoints(300)
            userPreferences.updateStreak(newStreak, history)
            mongoRepository.updateUserStats(profile.email, newPoints, newStreak, history)
        }
        
        stopFocusMode()
    }

    private suspend fun checkForegroundAndBlock() {
        val foregroundPkg = getForegroundPackage()
        if (foregroundPkg == null || foregroundPkg == packageName) return

        val profile = userPreferences.userProfileFlow.first()
        
        // 1. Check strict blocks (Focus Mode)
        if (isFocusModeActive && profile.blockedApps.contains(foregroundPkg)) {
            triggerBlockOverlay(foregroundPkg, "STRICT_BLOCK")
            return
        }

        // 2. Check App Limits
        val limit = profile.appLimits[foregroundPkg]
        if (limit != null && limit > 0) {
            val totalUsageToday = UsageStatsHelper.getAppUsageStats(this)
                .find { it.packageName == foregroundPkg }?.usageTimeMillis ?: 0L
            
            if (totalUsageToday >= limit) {
                triggerBlockOverlay(foregroundPkg, "LIMIT_EXCEEDED")
            } else if (limit - totalUsageToday <= 60000) {
                showWarningNotification(foregroundPkg, (limit - totalUsageToday) / 1000)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000
        
        val events = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun triggerBlockOverlay(packageName: String, reason: String) {
        val now = System.currentTimeMillis()
        if (lastBlockedPackage == packageName && now - lastBlockTime < 2000) return
        
        lastBlockedPackage = packageName
        lastBlockTime = now

        Log.d(TAG, "Triggering overlay for $packageName due to $reason")

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SCREEN", "nudge")
            putExtra("PACKAGE", packageName)
            putExtra("REASON", reason)
        }
        
        // Use a PendingIntent with high priority flags to force the launch
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            // This sends the intent directly, forcing the activity to launch
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            // Fallback to direct startActivity if PendingIntent fails
            startActivity(intent)
        }
        
        // Optional fallback: keep showing notification just in case OS blocks background launch
        showNudgeNotification(packageName, reason)
    }

    private fun showWarningNotification(pkgName: String, secondsLeft: Long) {
        val notification = NotificationCompat.Builder(this, "focus_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Stop Drifting!")
            .setContentText("You'll be over the limit for this app in $secondsLeft seconds.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(pkgName.hashCode() + 100, notification)
    }

    private fun showNudgeNotification(packageName: String, reason: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SCREEN", "nudge")
            putExtra("PACKAGE", packageName)
            putExtra("REASON", reason)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            packageName.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (reason == "LIMIT_EXCEEDED") "Time's Up!" else "Focus Alert!"
        val text = if (reason == "LIMIT_EXCEEDED") "Daily limit reached for this app." else "Close this app to stay focused."

        val notification = NotificationCompat.Builder(this, "focus_channel")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // This helps on many OS versions
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(packageName.hashCode(), notification)
    }

    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "focus_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "focus_channel",
                "Focus Mode Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used for app blocking and focus alerts"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
