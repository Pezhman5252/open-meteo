package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
        val TICKET_ID = stringPreferencesKey("ticket_id")
        // متنِ توضیحی که کاربر برای آخرین تیکت ارسال کرده — در «پیگیری تیکت» نمایش
        // داده می‌شود تا کاربر یادش نرود دقیقاً چه نوشته (ورکر description را هم
        // برمی‌گرداند؛ این ذخیره‌ی محلی فالبک برای تیکت‌های قدیمی است).
        val TICKET_DESCRIPTION = stringPreferencesKey("ticket_description")
        val UPDATE_LAST_SHOWN_AT = longPreferencesKey("update_last_shown_at")
        val UPDATE_DISMISSED_VERSION = stringPreferencesKey("update_dismissed_version")
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

    val ticketId: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[TICKET_ID] ?: ""
        }

    val ticketDescription: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[TICKET_DESCRIPTION] ?: ""
        }

    val updateLastShownAt: Flow<Long> = context.settingsDataStore.data
        .map { preferences ->
            preferences[UPDATE_LAST_SHOWN_AT] ?: 0L
        }

    val updateDismissedVersion: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[UPDATE_DISMISSED_VERSION] ?: ""
        }

    suspend fun setTicketId(id: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[TICKET_ID] = id
        }
    }

    /** Last ticket the user created — stores the description they wrote. */
    suspend fun setTicketDescription(description: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[TICKET_DESCRIPTION] = description
        }
    }

    /** Called when the update reminder is shown OR dismissed for a candidate version. */
    suspend fun markUpdateShown(candidateVersionCode: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[UPDATE_LAST_SHOWN_AT] = System.currentTimeMillis()
            preferences[UPDATE_DISMISSED_VERSION] = candidateVersionCode.toString()
        }
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
