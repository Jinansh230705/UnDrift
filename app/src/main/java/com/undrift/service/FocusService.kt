package com.undrift.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.undrift.MainActivity
import com.undrift.R
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

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "START_FOCUS" -> {
                val durationMinutes = intent.getIntExtra("DURATION", 60)
                startFocusMode(durationMinutes)
            }
            "STOP_FOCUS" -> stopFocusMode()
        }
        return START_STICKY
    }

    private fun startFocusMode(durationMinutes: Int) {
        isFocusModeActive = true
        focusEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
        
        val notification = createNotification("Focus Mode Active", "Stay focused for $durationMinutes minutes")
        startForeground(1, notification)
        
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isFocusModeActive) {
                checkAppUsage()
                checkAppLimits()
                
                if (System.currentTimeMillis() >= focusEndTime) {
                    completeFocusSession()
                    break
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    private fun stopFocusMode() {
        isFocusModeActive = false
        monitoringJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun completeFocusSession() {
        val profile = userPreferences.userProfileFlow.first()
        val newPoints = profile.points + 300
        val newStreak = profile.streakCount + 1
        
        // Update history
        val history = profile.streakHistory.toMutableList()
        if (history.isNotEmpty()) {
            history[history.size - 1] += profile.focusDurationMinutes
        }

        userPreferences.updatePoints(300)
        userPreferences.updateStreak(newStreak, history)
        
        // Sync to Mongo
        mongoRepository.updateUserStats(profile.email, newPoints, newStreak, history)
        
        stopFocusMode()
    }

    private suspend fun checkAppUsage() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
        
        if (stats != null) {
            val profile = userPreferences.userProfileFlow.first()
            val blockedApps = profile.blockedApps
            
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            if (sortedStats.isNotEmpty()) {
                val topApp = sortedStats[0].packageName
                if (blockedApps.contains(topApp) && topApp != packageName) {
                    showNudgeNotification(topApp)
                }
            }
        }
    }

    private suspend fun checkAppLimits() {
        val profile = userPreferences.userProfileFlow.first()
        if (profile.appLimits.isEmpty()) return

        val usageStats = UsageStatsHelper.getAppUsageStats(this)
        for (usage in usageStats) {
            val limit = profile.appLimits[usage.packageName]
            if (limit != null && limit > 0 && usage.usageTimeMillis >= limit) {
                if (!profile.appsExceededLimitToday.contains(usage.packageName)) {
                    userPreferences.markAppExceeded(usage.packageName)
                    showLimitExceededNotification(usage.appName, usage.packageName)
                }
            }
        }
    }

    private fun showLimitExceededNotification(appName: String, packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SCREEN", "nudge")
            putExtra("PACKAGE", packageName)
            putExtra("REASON", "LIMIT_EXCEEDED")
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, "focus_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Time's Up for $appName")
            .setContentText("You've reached your daily limit for this app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(packageName.hashCode(), notification)
    }

    private fun showNudgeNotification(packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SCREEN", "nudge")
            putExtra("PACKAGE", packageName)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, "focus_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Focus Alert!")
            .setContentText("You should close this app and stay focused.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification)
    }

    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "focus_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "focus_channel",
                "Focus Mode Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
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
