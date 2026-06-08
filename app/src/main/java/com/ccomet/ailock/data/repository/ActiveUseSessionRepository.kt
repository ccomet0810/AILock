package com.ccomet.ailock.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ccomet.ailock.data.local.ailockDataStore
import com.ccomet.ailock.data.model.ActiveUseSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

class ActiveUseSessionRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun save(session: ActiveUseSession) {
        context.ailockDataStore.edit { preferences ->
            val map = parseMap(preferences[ACTIVE_SESSIONS]).toMutableMap()
            map[session.packageName] = session
            preferences[ACTIVE_SESSIONS] = gson.toJson(map)
        }
    }

    suspend fun get(packageName: String): ActiveUseSession? {
        return parseMap(context.ailockDataStore.data.first()[ACTIVE_SESSIONS])[packageName]
    }

    suspend fun getAll(): Map<String, ActiveUseSession> {
        return parseMap(context.ailockDataStore.data.first()[ACTIVE_SESSIONS])
    }

    suspend fun clear(packageName: String) {
        context.ailockDataStore.edit { preferences ->
            val map = parseMap(preferences[ACTIVE_SESSIONS]).toMutableMap()
            map.remove(packageName)
            preferences[ACTIVE_SESSIONS] = gson.toJson(map)
        }
    }

    private fun parseMap(json: String?): Map<String, ActiveUseSession> = runCatching {
        if (json.isNullOrBlank()) {
            emptyMap()
        } else {
            val type = object : TypeToken<Map<String, ActiveUseSession>>() {}.type
            gson.fromJson<Map<String, ActiveUseSession>>(json, type).orEmpty()
        }
    }.getOrDefault(emptyMap())

    companion object {
        private val ACTIVE_SESSIONS = stringPreferencesKey("active_use_sessions_v1")
    }
}

