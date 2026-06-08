package com.ccomet.ailock.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.ccomet.ailock.AILockContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val timeLimitExceeded = intent?.getBooleanExtra(EXTRA_TIME_LIMIT_EXCEEDED, false) ?: false
        if (packageName.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (AILockOverlayController.isShowingFor(packageName)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            val container = AILockContainer.get(applicationContext)
            val config = container.ailockRepository.lockedApps.first()
                .firstOrNull { it.packageName == packageName }
            if (config != null) {
                AILockOverlayController.show(
                    context = applicationContext,
                    config = config,
                    timeLimitExceeded = timeLimitExceeded,
                    initialSession = container.activeUseSessionRepository.get(packageName),
                    initialPending = container.pendingFinalDecisionRepository.get(packageName),
                )
            }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val EXTRA_PACKAGE_NAME = "packageName"
        private const val EXTRA_TIME_LIMIT_EXCEEDED = "timeLimitExceeded"

        fun show(context: Context, packageName: String, timeLimitExceeded: Boolean) {
            val intent = Intent(context, OverlayService::class.java)
                .putExtra(EXTRA_PACKAGE_NAME, packageName)
                .putExtra(EXTRA_TIME_LIMIT_EXCEEDED, timeLimitExceeded)
            context.startService(intent)
        }
    }
}

