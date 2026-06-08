package com.ccomet.ailock.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ccomet.ailock.data.local.ailockDataStore
import com.ccomet.ailock.data.model.PendingFinalDecision
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

class PendingFinalDecisionRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun save(packageName: String, pending: PendingFinalDecision) {
        context.ailockDataStore.edit { preferences ->
            val map = parseMap(preferences[PENDING_DECISIONS]).toMutableMap()
            map[packageName] = pending
            preferences[PENDING_DECISIONS] = gson.toJson(map)
        }
    }

    suspend fun get(packageName: String): PendingFinalDecision? {
        return parseMap(context.ailockDataStore.data.first()[PENDING_DECISIONS])[packageName]
    }

    suspend fun clear(packageName: String) {
        context.ailockDataStore.edit { preferences ->
            val map = parseMap(preferences[PENDING_DECISIONS]).toMutableMap()
            map.remove(packageName)
            preferences[PENDING_DECISIONS] = gson.toJson(map)
        }
    }

    private fun parseMap(json: String?): Map<String, PendingFinalDecision> = runCatching {
        if (json.isNullOrBlank()) {
            emptyMap()
        } else {
            val type = object : TypeToken<Map<String, PendingFinalDecision>>() {}.type
            gson.fromJson<Map<String, PendingFinalDecision>>(json, type).orEmpty()
        }
    }.getOrDefault(emptyMap())

    companion object {
        private val PENDING_DECISIONS = stringPreferencesKey("pending_final_decisions_v1")
    }
}

