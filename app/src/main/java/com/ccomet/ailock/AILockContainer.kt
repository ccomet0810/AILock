package com.ccomet.ailock

import android.content.Context
import com.ccomet.ailock.data.repository.AILockRepository
import com.ccomet.ailock.data.repository.AiDecisionRepository
import com.ccomet.ailock.data.repository.PermissionRepository
import com.ccomet.ailock.domain.usecase.BlockingEngine
import com.ccomet.ailock.service.NotificationHelper
import com.ccomet.ailock.util.AppListLoader

class AILockContainer private constructor(context: Context) {
    private val appContext = context.applicationContext

    val ailockRepository = AILockRepository(appContext)
    val aiDecisionRepository = AiDecisionRepository()
    val permissionRepository = PermissionRepository(appContext)
    val appListLoader = AppListLoader(appContext)
    val notificationHelper = NotificationHelper(appContext)
    val blockingEngine = BlockingEngine(
        context = appContext,
        repository = ailockRepository,
        appListLoader = appListLoader,
    )

    companion object {
        @Volatile
        private var instance: AILockContainer? = null

        fun get(context: Context): AILockContainer = instance ?: synchronized(this) {
            instance ?: AILockContainer(context).also { instance = it }
        }
    }
}
