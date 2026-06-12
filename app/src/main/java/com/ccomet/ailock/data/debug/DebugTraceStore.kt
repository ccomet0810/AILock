package com.ccomet.ailock.data.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ccomet.ailock.data.local.ailockDataStore
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DebugTraceEntry(
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
)

object DebugTraceStore {
    private val DEBUG_MODE_ENABLED = booleanPreferencesKey("debug_mode_enabled")
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val prettyGson = GsonBuilder().setPrettyPrinting().create()
    private val _enabled = MutableStateFlow(false)
    private val _entries = MutableStateFlow<List<DebugTraceEntry>>(emptyList())

    val enabled: StateFlow<Boolean> = _enabled
    val entries: StateFlow<List<DebugTraceEntry>> = _entries

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.ailockDataStore.edit { preferences ->
            preferences[DEBUG_MODE_ENABLED] = enabled
        }
        _enabled.value = enabled
        if (!enabled) clear()
    }

    suspend fun syncEnabled(context: Context): Boolean {
        val enabled = context.applicationContext.ailockDataStore.data.first()[DEBUG_MODE_ENABLED] ?: false
        _enabled.value = enabled
        return enabled
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun append(title: String, body: String) {
        if (!_enabled.value) return
        _entries.value = (_entries.value + DebugTraceEntry(title, body)).takeLast(MAX_ENTRIES)
    }

    fun appendJson(title: String, value: Any?) {
        append(title, prettyGson.toJson(value))
    }

    fun allText(): String =
        _entries.value.joinToString(separator = "\n\n") { entry ->
            "[${timeFormatter.format(Date(entry.createdAt))}] ${entry.title}\n${entry.body}"
        }

    fun copyAll(context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("AILock debug trace", allText()))
    }

    private const val MAX_ENTRIES = 80
}
