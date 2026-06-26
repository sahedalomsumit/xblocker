package com.sahed.xblocker.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xblocker_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_BLOCKER_ACTIVE = booleanPreferencesKey("blocker_active")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_DEFAULT_LIST_ENABLED = booleanPreferencesKey("default_list_enabled")
        val KEY_AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        val KEY_UPSTREAM_DNS = stringPreferencesKey("upstream_dns")
        val KEY_BLOCKED_TODAY = intPreferencesKey("blocked_today")
        val KEY_BLOCKED_TODAY_DATE = longPreferencesKey("blocked_today_date")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val isBlockerActive: Flow<Boolean> = dataStore.data.map { it[KEY_BLOCKER_ACTIVE] ?: false }
    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_MODE] ?: true }
    val isOnboardingDone: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val isDefaultListEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_DEFAULT_LIST_ENABLED] ?: true }
    val isAutoStartBoot: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_START_BOOT] ?: true }
    val upstreamDns: Flow<String> = dataStore.data.map { it[KEY_UPSTREAM_DNS] ?: "1.1.1.1" }
    val blockedToday: Flow<Int> = dataStore.data.map { it[KEY_BLOCKED_TODAY] ?: 0 }

    suspend fun setBlockerActive(active: Boolean) = dataStore.edit { it[KEY_BLOCKER_ACTIVE] = active }
    suspend fun setDarkMode(enabled: Boolean) = dataStore.edit { it[KEY_DARK_MODE] = enabled }
    suspend fun setOnboardingDone() = dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    suspend fun setDefaultListEnabled(enabled: Boolean) = dataStore.edit { it[KEY_DEFAULT_LIST_ENABLED] = enabled }
    suspend fun setAutoStartBoot(enabled: Boolean) = dataStore.edit { it[KEY_AUTO_START_BOOT] = enabled }
    suspend fun setUpstreamDns(dns: String) = dataStore.edit { it[KEY_UPSTREAM_DNS] = dns }

    suspend fun incrementBlockedToday() = dataStore.edit {
        val today = System.currentTimeMillis() / 86_400_000L
        val storedDate = it[KEY_BLOCKED_TODAY_DATE] ?: 0L
        if (storedDate != today) {
            it[KEY_BLOCKED_TODAY] = 1
            it[KEY_BLOCKED_TODAY_DATE] = today
        } else {
            it[KEY_BLOCKED_TODAY] = (it[KEY_BLOCKED_TODAY] ?: 0) + 1
        }
    }

    suspend fun resetBlockedToday() = dataStore.edit {
        it[KEY_BLOCKED_TODAY] = 0
        it[KEY_BLOCKED_TODAY_DATE] = System.currentTimeMillis() / 86_400_000L
    }
}
