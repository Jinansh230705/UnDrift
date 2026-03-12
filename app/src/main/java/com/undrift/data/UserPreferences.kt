package com.undrift.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserProfile(
    val name: String,
    val email: String,
    val password: String = "",
    val age: String = "",
    val goal: String = "",
    val isLoggedIn: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val points: Int = 0,
    val streakCount: Int = 0,
    val streakHistory: List<Int> = emptyList(),
    val focusDurationMinutes: Int = 60,
    val lastExtraTimePurchaseDate: Long = 0L,
    val blockedApps: Set<String> = emptySet(),
    val appsExceededLimitToday: Set<String> = emptySet(),
    val appLimits: Map<String, Long> = emptyMap(), // Package name to limit in millis
    val lastStreakDate: Long = 0L,
    val themeColor: Long = 0xFFCE705D, // Default to Orange
    val demoMode: Boolean = false
)

class UserPreferences(private val context: Context) {
    companion object {
        private val NAME = stringPreferencesKey("user_name")
        private val EMAIL = stringPreferencesKey("user_email")
        private val PASSWORD = stringPreferencesKey("user_password")
        private val AGE = stringPreferencesKey("user_age")
        private val GOAL = stringPreferencesKey("user_goal")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val POINTS = intPreferencesKey("user_points")
        private val STREAK_COUNT = intPreferencesKey("streak_count")
        private val STREAK_HISTORY = stringPreferencesKey("streak_history")
        private val FOCUS_DURATION = intPreferencesKey("focus_duration")
        private val LAST_EXTRA_TIME_PURCHASE = longPreferencesKey("last_extra_time_purchase")
        private val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")
        private val EXCEEDED_APPS = stringSetPreferencesKey("exceeded_apps")
        private val APP_LIMITS = stringPreferencesKey("app_limits")
        private val LAST_RESET_DAY = intPreferencesKey("last_reset_day")
        private val LAST_STREAK_DATE = longPreferencesKey("last_streak_date")
        private val THEME_COLOR = longPreferencesKey("theme_color")
        private val DEMO_MODE = booleanPreferencesKey("demo_mode")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastReset = preferences[LAST_RESET_DAY] ?: -1
        
        val exceededApps = if (currentDay != lastReset) emptySet() else preferences[EXCEEDED_APPS] ?: emptySet()
        
        val appLimitsStr = preferences[APP_LIMITS] ?: ""
        val appLimits = appLimitsStr.split(",").filter { it.contains(":") }.associate { 
            val parts = it.split(":")
            parts[0] to (parts[1].toLongOrNull() ?: 0L)
        }

        // Auto-reset streak if no consecutive calendar days
        val rawStreak = preferences[STREAK_COUNT] ?: 0
        val lastStreakDateMillis = preferences[LAST_STREAK_DATE] ?: 0L
        val effectiveStreak = if (lastStreakDateMillis > 0 && rawStreak > 0) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val lastDayStart = Calendar.getInstance().apply {
                timeInMillis = lastStreakDateMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayDiff = (todayStart - lastDayStart) / (24 * 60 * 60 * 1000L)
            if (dayDiff > 1) 0 else rawStreak
        } else rawStreak

        UserProfile(
            name = preferences[NAME] ?: "",
            email = preferences[EMAIL] ?: "",
            password = preferences[PASSWORD] ?: "",
            age = preferences[AGE] ?: "",
            goal = preferences[GOAL] ?: "",
            isLoggedIn = preferences[IS_LOGGED_IN] ?: false,
            isFirstLaunch = preferences[IS_FIRST_LAUNCH] ?: true,
            points = preferences[POINTS] ?: 0,
            streakCount = effectiveStreak,
            streakHistory = (preferences[STREAK_HISTORY] ?: "0,0,0,0,0,0,0").split(",").map { it.toIntOrNull() ?: 0 },
            focusDurationMinutes = preferences[FOCUS_DURATION] ?: 60,
            lastExtraTimePurchaseDate = preferences[LAST_EXTRA_TIME_PURCHASE] ?: 0L,
            blockedApps = preferences[BLOCKED_APPS] ?: emptySet(),
            appsExceededLimitToday = exceededApps,
            appLimits = appLimits,
            lastStreakDate = lastStreakDateMillis,
            themeColor = preferences[THEME_COLOR] ?: 0xFFCE705D,
            demoMode = preferences[DEMO_MODE] ?: false
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[NAME] = profile.name
            preferences[EMAIL] = profile.email
            preferences[PASSWORD] = profile.password
            preferences[AGE] = profile.age
            preferences[GOAL] = profile.goal
            preferences[IS_LOGGED_IN] = true
            preferences[IS_FIRST_LAUNCH] = false
            preferences[POINTS] = profile.points
            preferences[STREAK_COUNT] = profile.streakCount
            preferences[STREAK_HISTORY] = profile.streakHistory.joinToString(",")
            preferences[FOCUS_DURATION] = profile.focusDurationMinutes
            preferences[LAST_EXTRA_TIME_PURCHASE] = profile.lastExtraTimePurchaseDate
            preferences[BLOCKED_APPS] = profile.blockedApps
            preferences[APP_LIMITS] = profile.appLimits.entries.joinToString(",") { "${it.key}:${it.value}" }
            preferences[LAST_STREAK_DATE] = profile.lastStreakDate
            preferences[THEME_COLOR] = profile.themeColor
        }
    }

    suspend fun setThemeColor(color: Long) {
        context.dataStore.edit { preferences ->
            preferences[THEME_COLOR] = color
        }
    }

    suspend fun updatePoints(points: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[POINTS] ?: 0
            preferences[POINTS] = current + points
        }
    }

    suspend fun updateStreak(streak: Int, history: List<Int>) {
        context.dataStore.edit { preferences ->
            preferences[STREAK_COUNT] = streak
            preferences[STREAK_HISTORY] = history.joinToString(",")
            preferences[LAST_STREAK_DATE] = System.currentTimeMillis()
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { preferences ->
            preferences[STREAK_COUNT] = 0
            preferences[LAST_STREAK_DATE] = 0L
        }
    }

    suspend fun deductPoints(amount: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[POINTS] ?: 0
            preferences[POINTS] = (current - amount).coerceAtLeast(0)
        }
    }

    suspend fun setFocusDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_DURATION] = minutes
        }
    }

    suspend fun addBlockedApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLOCKED_APPS] ?: emptySet()
            preferences[BLOCKED_APPS] = current + packageName
        }
    }

    suspend fun setAppLimit(packageName: String, limitMillis: Long) {
        context.dataStore.edit { preferences ->
            val appLimitsStr = preferences[APP_LIMITS] ?: ""
            val limits = appLimitsStr.split(",").filter { it.contains(":") }.associate { 
                val parts = it.split(":")
                parts[0] to (parts[1].toLongOrNull() ?: 0L)
            }.toMutableMap()
            limits[packageName] = limitMillis
            preferences[APP_LIMITS] = limits.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun markAppExceeded(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[EXCEEDED_APPS] ?: emptySet()
            preferences[EXCEEDED_APPS] = current + packageName
            preferences[LAST_RESET_DAY] = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        }
    }

    suspend fun recordExtraTimePurchase() {
        context.dataStore.edit { preferences ->
            preferences[LAST_EXTRA_TIME_PURCHASE] = System.currentTimeMillis()
            val currentPoints = preferences[POINTS] ?: 0
            preferences[POINTS] = currentPoints - 600
        }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }

    suspend fun setDemoMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEMO_MODE] = enabled
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
        }
    }
}
