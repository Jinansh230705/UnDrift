package com.undrift.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
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
    private var focusStartTime = 0L
    private var monitoringJob: Job? = null
    
    private val notificationTimestamps = mutableMapOf<String, Long>()

    // Overlay state
    private var overlayView: View? = null
    private var overlayBlockedPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Temporary app allowances (package -> expiry millis)
    private val tempAllowedApps = mutableMapOf<String, Long>()

    companion object {
        private const val TAG = "FocusService"
    }

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
        createNotificationChannel()
        val notification = createNotification("UnDrift Active", "Monitoring app usage")
        startForeground(1, notification)
        startBackgroundMonitoring()
    }

    private fun startBackgroundMonitoring() {
        serviceScope.launch {
            while (true) {
                try {
                    checkForegroundAndBlock()
                } catch (e: Exception) {
                    Log.e(TAG, "Monitor error: ${e.message}")
                }
                delay(1000)
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
            "STOP_FOCUS" -> stopFocusMode(breakStreak = true)
        }
        return START_STICKY
    }

    private fun startFocusMode(durationSeconds: Int) {
        isFocusModeActive = true
        focusStartTime = System.currentTimeMillis()
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

    private fun stopFocusMode(breakStreak: Boolean = false) {
        isFocusModeActive = false
        monitoringJob?.cancel()
        dismissOverlay()

        if (breakStreak) {
            serviceScope.launch {
                userPreferences.resetStreak()
            }
        }

        val notification = createNotification("UnDrift Active", "Monitoring app usage")
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, notification)
    }

    private suspend fun completeFocusSession() {
        val profile = userPreferences.userProfileFlow.first()

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastDate = profile.lastStreakDate
        val lastDayStart = if (lastDate > 0) {
            Calendar.getInstance().apply {
                timeInMillis = lastDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else 0L

        val oneDayMillis = 24 * 60 * 60 * 1000L
        val newStreak = when {
            lastDayStart == todayStart -> profile.streakCount // already counted today
            todayStart - lastDayStart == oneDayMillis -> profile.streakCount + 1
            else -> 1 // gap or first ever
        }

        userPreferences.updateStreak(newStreak, profile.streakHistory)
        userPreferences.updatePoints(300)

        val newPoints = profile.points + 300
        mongoRepository.updateUserStats(profile.email, newPoints, newStreak, profile.streakHistory)

        stopFocusMode(breakStreak = false)
    }

    // ─── Core monitoring logic ───────────────────────────────────────────

    private suspend fun checkForegroundAndBlock() {
        // Overlay is active → don't interfere, user must dismiss manually
        if (overlayView != null) return

        val foregroundPkg = getForegroundPackage()
        if (foregroundPkg == null || foregroundPkg == packageName) return

        // Check temp allowance
        val tempExpiry = tempAllowedApps[foregroundPkg]
        if (tempExpiry != null) {
            if (System.currentTimeMillis() < tempExpiry) return
            else tempAllowedApps.remove(foregroundPkg)
        }

        val profile = userPreferences.userProfileFlow.first()

        // 1. Focus Mode strict block
        if (isFocusModeActive && profile.blockedApps.contains(foregroundPkg)) {
            showOverlay(foregroundPkg, "STRICT_BLOCK", profile.points)
            return
        }

        // 2. App time limit exceeded
        val limit = profile.appLimits[foregroundPkg]
        if (limit != null && limit > 0) {
            val usageToday = UsageStatsHelper.getAppUsageToday(this, foregroundPkg)
            Log.d(TAG, "Usage check: $foregroundPkg used ${usageToday/1000}s, limit ${limit/1000}s")

            if (usageToday >= limit) {
                showOverlay(foregroundPkg, "LIMIT_EXCEEDED", profile.points)
                return
            } else if (limit - usageToday <= 60_000) {
                showWarningNotification(foregroundPkg, (limit - usageToday) / 1000)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 30_000  // 30s window for reliability

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

    // ─── WindowManager overlay (the actual blocking UI) ──────────────────

    private fun showOverlay(blockedPkg: String, reason: String, userPoints: Int = 0) {
        // Already showing for this package
        if (overlayView != null && overlayBlockedPackage == blockedPkg) return

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "No SYSTEM_ALERT_WINDOW permission – falling back to notification")
            showFallbackNotification(blockedPkg, reason)
            return
        }

        // Dismiss any previous overlay first
        dismissOverlay()

        mainHandler.post {
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.CENTER

                val view = buildOverlayView(blockedPkg, reason, userPoints)
                wm.addView(view, params)
                overlayView = view
                overlayBlockedPackage = blockedPkg
                Log.d(TAG, "Overlay shown for $blockedPkg ($reason)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay: ${e.message}")
                showFallbackNotification(blockedPkg, reason)
            }
        }
    }

    private fun dismissOverlay() {
        if (overlayView == null) return
        mainHandler.post {
            try {
                overlayView?.let {
                    val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                    wm.removeView(it)
                    Log.d(TAG, "Overlay dismissed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay: ${e.message}")
            }
            overlayView = null
            overlayBlockedPackage = null
        }
    }

    private fun grantTempAccess(packageName: String, durationMinutes: Int) {
        tempAllowedApps[packageName] = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        dismissOverlay()
        serviceScope.launch {
            userPreferences.deductPoints(300)
        }
    }

    private fun buildOverlayView(blockedPkg: String, reason: String, userPoints: Int): View {
        val ctx = this
        val dp = { value: Int ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), ctx.resources.displayMetrics).toInt()
        }
        val dpf = { value: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, ctx.resources.displayMetrics)
        }

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(blockedPkg, 0)).toString()
        } catch (_: Exception) { blockedPkg }

        val focusMinutes = if (isFocusModeActive && focusStartTime > 0) {
            ((System.currentTimeMillis() - focusStartTime) / 60_000).toInt().coerceAtLeast(1)
        } else 0

        val accentColor = 0xFFCE705D.toInt()

        // Root: full screen semi-transparent dark background
        val root = FrameLayout(ctx).apply {
            setBackgroundColor(0xB3000000.toInt())
        }

        // Bottom sheet card
        val sheetBg = GradientDrawable().apply {
            setColor(0xFF1E1E1E.toInt())
            cornerRadii = floatArrayOf(dpf(28f), dpf(28f), dpf(28f), dpf(28f), 0f, 0f, 0f, 0f)
        }
        val sheet = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = sheetBg
            setPadding(dp(28), dp(16), dp(28), dp(40))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM }
        }

        // Handle pill
        val handle = View(ctx).apply {
            val bg = GradientDrawable().apply {
                setColor(0xFF666666.toInt())
                cornerRadius = dpf(3f)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(5)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(20)
            }
        }
        sheet.addView(handle)

        // "Focus Agent" title
        val title = TextView(ctx).apply {
            text = "Focus Agent"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(8)
            }
        }
        sheet.addView(title)

        // Badge: "● FOCUS MODE ACTIVE"
        val badge = TextView(ctx).apply {
            text = "\u25CF  FOCUS MODE ACTIVE"
            setTextColor(accentColor)
            textSize = 11f
            letterSpacing = 0.12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(0x33CE705D)
                cornerRadius = dpf(12f)
            }
            background = bg
            setPadding(dp(14), dp(6), dp(14), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(24)
            }
        }
        sheet.addView(badge)

        // Main message
        val mainMsg = TextView(ctx).apply {
            text = if (reason == "STRICT_BLOCK" && focusMinutes > 0) {
                "You\u2019ve been focused for $focusMinutes mins\u2014keep the momentum?"
            } else if (reason == "LIMIT_EXCEEDED") {
                "Time\u2019s up for $appName\u2014stay on track!"
            } else {
                "Stay focused\u2014keep the momentum?"
            }
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        sheet.addView(mainMsg)

        // Subtitle
        val subtitle = TextView(ctx).apply {
            text = if (reason == "STRICT_BLOCK") {
                "I noticed you\u2019re exploring content that might break your current deep work streak."
            } else {
                "You\u2019ve reached your daily limit for $appName. Close and stay focused on your goals."
            }
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 14f
            gravity = Gravity.CENTER
            setLineSpacing(dpf(4f), 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(28) }
        }
        sheet.addView(subtitle)

        // "⚡ Back to Focus" button (primary accent, rounded)
        val backBtn = Button(ctx).apply {
            text = "\u26A1 Back to Focus"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            val bg = GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = dpf(16f)
            }
            background = bg
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
            ).apply { bottomMargin = dp(12) }
            setOnClickListener {
                // Award 15 focus points for returning
                serviceScope.launch { userPreferences.updatePoints(15) }
                dismissOverlay()
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }
        sheet.addView(backBtn)

        // "I need 20 min" button (light/cream, rounded)
        val hasEnoughCoins = userPoints >= 300
        val extraBtn = Button(ctx).apply {
            text = "I need 20 min"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (hasEnoughCoins) 0xFF1E1E1E.toInt() else 0xFF888888.toInt())
            val bg = GradientDrawable().apply {
                setColor(if (hasEnoughCoins) 0xFFE8D5C4.toInt() else 0xFF3A3A3A.toInt())
                cornerRadius = dpf(16f)
            }
            background = bg
            isEnabled = hasEnoughCoins
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
            ).apply { bottomMargin = dp(20) }
            setOnClickListener {
                grantTempAccess(blockedPkg, 20)
            }
        }
        sheet.addView(extraBtn)

        // Stats line
        val statsLine = TextView(ctx).apply {
            text = "\u2B50  92% of users stay on track after this nudge"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        sheet.addView(statsLine)

        // "+15 FOCUS POINTS FOR RETURNING NOW" pill
        val pointsBadge = TextView(ctx).apply {
            text = "+15 FOCUS POINTS FOR RETURNING NOW"
            setTextColor(accentColor)
            textSize = 11f
            letterSpacing = 0.06f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(0x33CE705D)
                cornerRadius = dpf(12f)
            }
            background = bg
            setPadding(dp(14), dp(6), dp(14), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }
        sheet.addView(pointsBadge)

        root.addView(sheet)
        return root
    }

    // ─── Notifications ───────────────────────────────────────────────────

    private fun showWarningNotification(pkgName: String, secondsLeft: Long) {
        val now = System.currentTimeMillis()
        val lastWarning = notificationTimestamps[pkgName + "_warning"] ?: 0L
        if (now - lastWarning < 60_000) return
        notificationTimestamps[pkgName + "_warning"] = now

        val notification = NotificationCompat.Builder(this, "focus_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏰ Time's Running Out")
            .setContentText("$secondsLeft seconds left for this app today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(pkgName.hashCode() + 100, notification)
    }

    private fun showFallbackNotification(blockedPkg: String, reason: String) {
        val now = System.currentTimeMillis()
        val key = blockedPkg + "_fallback"
        val last = notificationTimestamps[key] ?: 0L
        if (now - last < 10_000) return
        notificationTimestamps[key] = now

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SCREEN", "nudge")
            putExtra("PACKAGE", blockedPkg)
            putExtra("REASON", reason)
        }
        val pi = PendingIntent.getActivity(this, blockedPkg.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val title = if (reason == "LIMIT_EXCEEDED") "⏹ Time's Up!" else "🎯 Focus Alert!"
        val text = if (reason == "LIMIT_EXCEEDED") "You've reached your limit. Tap to block."
                   else "This app is blocked. Tap to go back."

        val notification = NotificationCompat.Builder(this, "focus_channel")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(false)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(blockedPkg.hashCode(), notification)
    }

    // ─── Notification channel & service plumbing ─────────────────────────

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
        dismissOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }
}
