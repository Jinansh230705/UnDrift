package com.undrift.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import java.util.*

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val icon: android.graphics.drawable.Drawable?
)

object UsageStatsHelper {
    private const val TAG = "UsageStatsHelper"

    /**
     * Filters for "real" apps that the user actually interacts with.
     * This avoids the "9hr vs 4.5hr" issue where system components and background 
     * services inflate the total.
     */
    private fun isUserFacingApp(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        
        val pm = context.packageManager
        
        // Exclude known system packages that don't have a UI but might report foreground time
        val systemExclusions = listOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.gms",
            "com.google.android.inputmethod.latin",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher",
            "com.android.launcher3",
            "com.sec.android.app.launcher",
            "com.miui.home"
        )
        if (systemExclusions.contains(packageName)) return false

        // Crucial: Only count apps that can be launched by the user (have an icon in launcher)
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) return false

        // Exclude the current active launcher/home screen
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo?.activityInfo?.packageName == packageName) return false

        return true
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

        // queryAndAggregateUsageStats summarizes by package for the given range
        val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        return statsMap.mapNotNull { (packageName, usageStats) ->
            if (!isUserFacingApp(context, packageName)) return@mapNotNull null
            
            val usageTime = usageStats.totalTimeInForeground
            if (usageTime <= 5000) return@mapNotNull null // Ignore apps used for less than 5 seconds

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }

            AppUsageInfo(
                packageName = packageName,
                appName = appName,
                usageTimeMillis = usageTime,
                icon = try { packageManager.getApplicationIcon(packageName) } catch (e: Exception) { null }
            )
        }.sortedByDescending { it.usageTimeMillis }
    }

    fun getScreenTimeToday(context: Context): Long {
        // Calculate total only from filtered apps to ensure consistency with the breakdown
        val stats = getAppUsageStats(context)
        val total = stats.sumOf { it.usageTimeMillis }
        Log.d(TAG, "Consistent Total: ${total / 60000} mins")
        return total
    }

    fun getInstalledApps(context: Context): List<AppUsageInfo> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApps.mapNotNull { appInfo ->
            if (!isUserFacingApp(context, appInfo.packageName)) return@mapNotNull null

            AppUsageInfo(
                packageName = appInfo.packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                usageTimeMillis = 0,
                icon = try { packageManager.getApplicationIcon(appInfo) } catch (e: Exception) { null }
            )
        }.sortedBy { it.appName }
    }
}
