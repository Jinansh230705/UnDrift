package com.undrift.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.*

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val icon: android.graphics.drawable.Drawable?
)

object UsageStatsHelper {
    fun getScreenTimeToday(context: Context): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        // queryAndAggregateUsageStats is more reliable for current day totals
        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        return stats.values.sumOf { it.totalTimeInForeground }
    }

    fun getAppUsageStats(context: Context): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApps.filter { 
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || 
            it.packageName == "com.android.chrome" || 
            it.packageName == "com.android.vending" ||
            it.packageName == "com.google.android.youtube"
        }.map { appInfo ->
            val usageTime = stats[appInfo.packageName]?.totalTimeInForeground ?: 0L
            AppUsageInfo(
                packageName = appInfo.packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                usageTimeMillis = usageTime,
                icon = try { packageManager.getApplicationIcon(appInfo) } catch (e: Exception) { null }
            )
        }.sortedByDescending { it.usageTimeMillis }
    }

    fun getInstalledApps(context: Context): List<AppUsageInfo> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApps.filter { 
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 
        }.map { appInfo ->
            AppUsageInfo(
                packageName = appInfo.packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                usageTimeMillis = 0,
                icon = try { packageManager.getApplicationIcon(appInfo) } catch (e: Exception) { null }
            )
        }.sortedBy { it.appName }
    }
}
