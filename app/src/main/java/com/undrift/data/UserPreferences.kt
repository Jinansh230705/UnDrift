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
    val appLimits: Map<String, Long> = emptyMap() // Package name to limit in millis
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
        private val APP_LIMITS = stringPreferencesKey("app_limits") // JSON or CSV: pkg1:limit1,pkg2:limit2
        private val LAST_RESET_DAY = intPreferencesKey("last_reset_day")
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

        UserProfile(
            name = preferences[NAME] ?: "",
            email = preferences[EMAIL] ?: "",
            password = preferences[PASSWORD] ?: "",
            age = preferences[AGE] ?: "",
            goal = preferences[GOAL] ?: "",
            isLoggedIn = preferences[IS_LOGGED_IN] ?: false,
            isFirstLaunch = preferences[IS_FIRST_LAUNCH] ?: true,
            points = preferences[POINTS] ?: 0,
            streakCount = preferences[STREAK_COUNT] ?: 0,
            streakHistory = (preferences[STREAK_HISTORY] ?: "0,0,0,0,0,0,0").split(",").map { it.toIntOrNull() ?: 0 },
            focusDurationMinutes = preferences[FOCUS_DURATION] ?: 60,
            lastExtraTimePurchaseDate = preferences[LAST_EXTRA_TIME_PURCHASE] ?: 0L,
            blockedApps = preferences[BLOCKED_APPS] ?: emptySet(),
            appsExceededLimitToday = exceededApps,
            appLimits = appLimits
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

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
        }
    }
}
