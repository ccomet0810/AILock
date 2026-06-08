package com.ccomet.ailock.util

import android.content.Context
import com.ccomet.ailock.data.model.PermissionState
import com.ccomet.ailock.data.repository.PermissionRepository

object PermissionUtils {
    fun currentState(context: Context): PermissionState =
        PermissionRepository(context.applicationContext).currentState()
}

