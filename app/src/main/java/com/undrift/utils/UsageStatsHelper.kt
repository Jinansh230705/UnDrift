package com.undrift.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.*

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val iconBitmap: ImageBitmap? = null
)

private data class AppCacheEntry(
    var appName: String? = null,
    var iconBitmap: ImageBitmap? = null,
    val isSystemAndNotUpdated: Boolean,
    val hasLaunchIntent: Boolean,
    var fullyLoaded: Boolean = false
)

object UsageStatsHelper {
    private const val TAG = "UsageStatsHelper"

    private val systemExclusions = setOf(
        "android",
        "com.android.systemui"
    )

    private val appInfoCache = mutableMapOf<String, AppCacheEntry>()

    private fun getAppCacheEntry(context: Context, packageName: String, loadFullDetails: Boolean): AppCacheEntry {
        appInfoCache[packageName]?.let {
            if (!loadFullDetails || it.fullyLoaded) return it
        }
        val packageManager = context.packageManager
        val hasLaunchIntent = packageManager.getLaunchIntentForPackage(packageName) != null
        
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdated = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val isSystemAndNotUpdated = isSystem && !isUpdated
            
            var appName: String? = null
            var iconBitmap: ImageBitmap? = null
            
            if (loadFullDetails) {
                appName = if (packageName == context.packageName) "UnDrift" else packageManager.getApplicationLabel(appInfo).toString()
                iconBitmap = try { packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap() } catch (e: Exception) { null }
            }
            
            val entry = appInfoCache[packageName] ?: AppCacheEntry(
                appName = appName,
                iconBitmap = iconBitmap,
                isSystemAndNotUpdated = isSystemAndNotUpdated,
                hasLaunchIntent = hasLaunchIntent,
                fullyLoaded = loadFullDetails
            )
            
            if (loadFullDetails && !entry.fullyLoaded) {
                entry.appName = appName
                entry.iconBitmap = iconBitmap
                entry.fullyLoaded = true
            }
            
            appInfoCache[packageName] = entry
            return entry
        } catch (e: Exception) {
            val entry = AppCacheEntry(
                appName = if (packageName == context.packageName) "UnDrift" else packageName,
                iconBitmap = null,
                isSystemAndNotUpdated = false,
                hasLaunchIntent = hasLaunchIntent,
                fullyLoaded = loadFullDetails
            )
            appInfoCache[packageName] = entry
            return entry
        }
    }

    private fun calculateForegroundTimesFromEvents(
        usm: UsageStatsManager, startTime: Long, endTime: Long
    ): Map<String, Long> {
        val events = usm.queryEvents(startTime, endTime) ?: return emptyMap()
        val event = UsageEvents.Event()
        val foregroundTimes = mutableMapOf<String, Long>()
        val lastResumed = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumed[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val resumeTime = lastResumed.remove(pkg)
                    if (resumeTime != null && event.timeStamp > resumeTime) {
                        foregroundTimes[pkg] = (foregroundTimes[pkg] ?: 0L) + (event.timeStamp - resumeTime)
                    }
                }
            }
        }

        for ((pkg, resumeTime) in lastResumed) {
            if (endTime > resumeTime) {
                foregroundTimes[pkg] = (foregroundTimes[pkg] ?: 0L) + (endTime - resumeTime)
            }
        }

        return foregroundTimes
    }

    fun getAppUsageToday(context: Context, targetPackage: String): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val events = usm.queryEvents(startTime, endTime) ?: return 0L
        val event = UsageEvents.Event()
        var totalTime = 0L
        var lastResumedTime: Long? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != targetPackage) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (lastResumedTime == null) {
                        lastResumedTime = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (lastResumedTime != null) {
                        totalTime += event.timeStamp - lastResumedTime
                        lastResumedTime = null
                    }
                }
            }
        }

        if (lastResumedTime != null) {
            totalTime += endTime - lastResumedTime
        }

        return totalTime
    }

    fun getAppUsageStats(context: Context): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val foregroundTimes = calculateForegroundTimesFromEvents(usageStatsManager, startTime, endTime)
        
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncher = resolveInfo?.activityInfo?.packageName

        return foregroundTimes.mapNotNull { (packageName, usageTime) ->
            if (systemExclusions.contains(packageName)) return@mapNotNull null
            if (packageName == currentLauncher) return@mapNotNull null
            if (usageTime <= 1000) return@mapNotNull null

            val cacheEntry = getAppCacheEntry(context, packageName, loadFullDetails = true)
            if (!cacheEntry.hasLaunchIntent && packageName != context.packageName) return@mapNotNull null

            AppUsageInfo(
                packageName = packageName,
                appName = cacheEntry.appName ?: if (packageName == context.packageName) "UnDrift" else packageName,
                usageTimeMillis = usageTime,
                iconBitmap = cacheEntry.iconBitmap
            )
        }.sortedByDescending { it.usageTimeMillis }
    }

    fun getScreenTimeToday(context: Context): Long {
        val stats = getAppUsageStats(context)
        return stats.sumOf { it.usageTimeMillis }
    }

    fun getWeeklyDailyScreenTime(context: Context): List<Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncher = resolveInfo?.activityInfo?.packageName

        val result = mutableListOf<Long>()
        val today = Calendar.getInstance()

        for (daysAgo in 6 downTo 0) {
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = today.timeInMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayEnd = if (daysAgo == 0) {
                System.currentTimeMillis()
            } else {
                Calendar.getInstance().apply {
                    timeInMillis = dayStart.timeInMillis
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
            }

            val foregroundTimes = calculateForegroundTimesFromEvents(usm, dayStart.timeInMillis, dayEnd)
            var dayTotal = 0L
            for ((pkg, time) in foregroundTimes) {
                if (systemExclusions.contains(pkg)) continue
                if (pkg == currentLauncher) continue
                if (time <= 1000) continue
                
                val cacheEntry = getAppCacheEntry(context, pkg, loadFullDetails = false)
                if (!cacheEntry.hasLaunchIntent && pkg != context.packageName) continue
                
                dayTotal += time
            }
            result.add(dayTotal)
        }
        return result
    }

    fun getInstalledApps(context: Context): List<AppUsageInfo> {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val launchables = packageManager.queryIntentActivities(mainIntent, 0)
        
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncher = resolveInfo?.activityInfo?.packageName

        return launchables.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            if (systemExclusions.contains(packageName)) return@mapNotNull null
            if (packageName == currentLauncher) return@mapNotNull null

            AppUsageInfo(
                packageName = packageName,
                appName = if (packageName == context.packageName) "UnDrift" else resolveInfo.loadLabel(packageManager).toString(),
                usageTimeMillis = 0,
                iconBitmap = try { resolveInfo.loadIcon(packageManager).toBitmap().asImageBitmap() } catch (e: Exception) { null }
            )
        }.distinctBy { it.packageName }.sortedBy { it.appName }
    }

    fun getDemoWeeklyScreenTime(): List<Long> {
        val cal = Calendar.getInstance()
        val weekSeed = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR)
        val rng = java.util.Random(weekSeed.toLong())
        return (0 until 7).map {
            val minMs = 3 * 3_600_000L
            val rangeMs = 2 * 3_600_000L
            minMs + (rng.nextDouble() * rangeMs).toLong()
        }
    }

    fun getDemoAppUsageStats(context: Context): List<AppUsageInfo> {
        val real = getAppUsageStats(context)
        if (real.isEmpty()) return real

        val todayTotal = getDemoWeeklyScreenTime().last()
        val realTotal = real.sumOf { it.usageTimeMillis }.coerceAtLeast(1L)
        val scale = todayTotal.toDouble() / realTotal

        return real.map { app ->
            app.copy(usageTimeMillis = (app.usageTimeMillis * scale).toLong().coerceAtLeast(0L))
        }.filter { it.usageTimeMillis > 30_000 }
         .sortedByDescending { it.usageTimeMillis }
    }
}
