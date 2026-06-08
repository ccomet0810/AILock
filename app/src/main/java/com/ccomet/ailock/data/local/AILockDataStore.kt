package com.ccomet.ailock.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.ailockDataStore by preferencesDataStore(name = "ailock_settings")

