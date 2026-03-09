package com.undrift.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserProfile(
    val name: String,
    val email: String,
    val age: String,
    val goal: String,
    val isLoggedIn: Boolean = false
)

class UserPreferences(private val context: Context) {
    companion object {
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val AGE = stringPreferencesKey("user_age")
        val GOAL = stringPreferencesKey("user_goal")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        UserProfile(
            name = preferences[NAME] ?: "",
            email = preferences[EMAIL] ?: "",
            age = preferences[AGE] ?: "",
            goal = preferences[GOAL] ?: "",
            isLoggedIn = preferences[IS_LOGGED_IN] ?: false
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[NAME] = profile.name
            preferences[EMAIL] = profile.email
            preferences[AGE] = profile.age
            preferences[GOAL] = profile.goal
            preferences[IS_LOGGED_IN] = true
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
        }
    }
}
