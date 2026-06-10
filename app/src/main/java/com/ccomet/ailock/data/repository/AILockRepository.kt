package com.ccomet.ailock.data.repository

import android.content.Context
import com.ccomet.ailock.BuildConfig
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ccomet.ailock.data.local.ailockDataStore
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.UsageEventType
import com.ccomet.ailock.data.model.UsageRecord
import com.ccomet.ailock.data.model.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class AILockRepository(private val context: Context) {
    private val gson = Gson()
    private val temporaryAllowances = MutableStateFlow<Map<String, Long>>(emptyMap())

    val onboardingCompleted: Flow<Boolean> = context.ailockDataStore.data
        .map { it[ONBOARDING_COMPLETED] ?: false }

    val userProfile: Flow<UserProfile> = context.ailockDataStore.data
        .map { preferences -> parseUserProfile(preferences[USER_PROFILE]) }

    val lockedApps: Flow<List<LockedAppConfig>> = context.ailockDataStore.data
        .map { preferences -> parseLockedApps(preferences[LOCKED_APPS]) }

    val usageRecords: Flow<List<UsageRecord>> = context.ailockDataStore.data
        .map { preferences -> parseUsageRecords(preferences[USAGE_RECORDS]) }

    val willPowerScore: Flow<Int> = context.ailockDataStore.data
        .map { preferences -> preferences[WILL_POWER_SCORE] ?: DEFAULT_WILL_POWER_SCORE }

    val backendBaseUrl: Flow<String> = context.ailockDataStore.data
        .map { preferences -> normalizeBackendBaseUrl(preferences[BACKEND_BASE_URL] ?: DEFAULT_BACKEND_BASE_URL) }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.ailockDataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.ailockDataStore.edit { it[USER_PROFILE] = gson.toJson(profile) }
    }

    suspend fun saveBackendBaseUrl(url: String) {
        context.ailockDataStore.edit { preferences ->
            preferences[BACKEND_BASE_URL] = normalizeBackendBaseUrl(url)
        }
    }

    suspend fun upsertLockedApp(config: LockedAppConfig) {
        context.ailockDataStore.edit { preferences ->
            val current = parseLockedApps(preferences[LOCKED_APPS]).toMutableList()
            val index = current.indexOfFirst { it.id == config.id }
            val saved = config.copy(updatedAt = System.currentTimeMillis())
            if (index >= 0) {
                current[index] = saved
            } else {
                current += saved
            }
            preferences[LOCKED_APPS] = gson.toJson(current)
        }
    }

    suspend fun deleteLockedApp(id: Long) {
        context.ailockDataStore.edit { preferences ->
            val next = parseLockedApps(preferences[LOCKED_APPS]).filterNot { it.id == id }
            preferences[LOCKED_APPS] = gson.toJson(next)
        }
    }

    suspend fun addUsageRecord(record: UsageRecord) {
        context.ailockDataStore.edit { preferences ->
            val next = (parseUsageRecords(preferences[USAGE_RECORDS]) + record)
                .sortedByDescending { it.openedAt }
                .take(MAX_STORED_RECORDS)
            preferences[USAGE_RECORDS] = gson.toJson(next)
        }
    }

    suspend fun adjustWillPower(delta: Int) {
        context.ailockDataStore.edit { preferences ->
            val next = ((preferences[WILL_POWER_SCORE] ?: DEFAULT_WILL_POWER_SCORE) + delta)
                .coerceIn(0, 100)
            preferences[WILL_POWER_SCORE] = next
        }
    }

    fun grantTemporaryAllowance(packageName: String, minutes: Int) {
        if (minutes <= 0) return
        temporaryAllowances.value = temporaryAllowances.value + (
            packageName to System.currentTimeMillis() + minutes * 60_000L
        )
    }

    fun hasActiveTemporaryAllowance(packageName: String): Boolean {
        val expiresAt = temporaryAllowances.value[packageName] ?: return false
        val active = System.currentTimeMillis() < expiresAt
        if (!active) {
            temporaryAllowances.value = temporaryAllowances.value - packageName
        }
        return active
    }

    suspend fun recordEvent(
        packageName: String,
        appName: String,
        eventType: UsageEventType,
        durationMinutes: Int = 0,
        aiStatus: String? = null,
        aiAllowedTime: Int? = null,
        userInput: String? = null,
        lockReason: String? = null,
    ) {
        addUsageRecord(
            UsageRecord(
                packageName = packageName,
                appName = appName,
                durationMinutes = durationMinutes,
                eventType = eventType,
                aiStatus = aiStatus,
                aiAllowedTime = aiAllowedTime,
                userInput = userInput,
                lockReason = lockReason,
            ),
        )
    }

    private fun parseUserProfile(json: String?): UserProfile = runCatching {
        if (json.isNullOrBlank()) UserProfile() else gson.fromJson(json, UserProfile::class.java)
    }.getOrDefault(UserProfile())

    private fun parseLockedApps(json: String?): List<LockedAppConfig> = runCatching {
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<LockedAppConfig>>() {}.type
            gson.fromJson<List<LockedAppConfig>>(json, type).orEmpty()
        }
    }.getOrDefault(emptyList())

    private fun parseUsageRecords(json: String?): List<UsageRecord> = runCatching {
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<UsageRecord>>() {}.type
            gson.fromJson<List<UsageRecord>>(json, type).orEmpty()
        }
    }.getOrDefault(emptyList())

    private fun normalizeBackendBaseUrl(url: String): String {
        val trimmed = url.trim().ifBlank { DEFAULT_BACKEND_BASE_URL }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val USER_PROFILE = stringPreferencesKey("user_profile")
        private val LOCKED_APPS = stringPreferencesKey("locked_apps")
        private val USAGE_RECORDS = stringPreferencesKey("usage_records")
        private val WILL_POWER_SCORE = intPreferencesKey("will_power_score")
        private val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
        private val DEFAULT_BACKEND_BASE_URL = BuildConfig.AILOCK_BACKEND_BASE_URL
        private const val MAX_STORED_RECORDS = 500
        private const val DEFAULT_WILL_POWER_SCORE = 80
    }
}

