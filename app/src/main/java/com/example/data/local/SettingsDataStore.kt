package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extends Context to provide dataStore instance
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

class SettingsDataStore(private val context: Context) {

    companion object {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACTIVATION_CODE = stringPreferencesKey("activation_code")
        val SUBSCRIPTION_ID = stringPreferencesKey("subscription_id")
        val SUBSCRIPTION_EXPIRES_AT = stringPreferencesKey("subscription_expires_at")
    }

    val isPremium: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[IS_PREMIUM] ?: false
        }

    val themeMode: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: "system"
        }

    val activationCode: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[ACTIVATION_CODE] ?: ""
        }

    val subscriptionId: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[SUBSCRIPTION_ID] ?: ""
        }

    val subscriptionExpiresAt: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[SUBSCRIPTION_EXPIRES_AT] ?: ""
        }

    suspend fun setPremium(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_PREMIUM] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setActivationDetails(code: String, subId: String, expiresAt: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[ACTIVATION_CODE] = code
            preferences[SUBSCRIPTION_ID] = subId
            preferences[SUBSCRIPTION_EXPIRES_AT] = expiresAt
            preferences[IS_PREMIUM] = true
        }
    }

    suspend fun clearActivationDetails() {
        context.settingsDataStore.edit { preferences ->
            preferences[ACTIVATION_CODE] = ""
            preferences[SUBSCRIPTION_ID] = ""
            preferences[SUBSCRIPTION_EXPIRES_AT] = ""
            preferences[IS_PREMIUM] = false
        }
    }
}
