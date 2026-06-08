package com.ccomet.ailock.data.work

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.service.AILockAccessibilityService
import com.ccomet.ailock.service.AILockOverlayController

class SessionExitWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        val container = AILockContainer.get(applicationContext)
        val session = container.activeUseSessionRepository.get(packageName) ?: return Result.success()

        if (session.state != SESSION_STATE_IN_USE) {
            return Result.success()
        }

        if (AILockOverlayController.isShowingFor(packageName)) {
            return Result.success()
        }

        if (currentForegroundPackage() == packageName) {
            return Result.success()
        }

        if (session.sessionId.isNotBlank()) {
            val totalMinutes = ((System.currentTimeMillis() - session.startedAt) / 60_000L)
                .toInt()
                .coerceAtLeast(0)
            SessionWorkScheduler.enqueueSessionUpdate(applicationContext, session.sessionId, totalMinutes)
        }

        container.pendingFinalDecisionRepository.clear(packageName)
        container.activeUseSessionRepository.clear(packageName)
        return Result.success()
    }

    private fun currentForegroundPackage(): String? {
        AILockAccessibilityService.currentForegroundPackage()?.let { return it }
        val manager = applicationContext.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - LOOKBACK_MS, now)
        val event = android.app.usage.UsageEvents.Event()
        var foregroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED,
                usageMoveToForegroundEvent(),
                -> foregroundPackage = event.packageName

                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                usageMoveToBackgroundEvent(),
                -> if (foregroundPackage == event.packageName) {
                    foregroundPackage = null
                }
            }
        }

        return foregroundPackage
    }

    @Suppress("DEPRECATION")
    private fun usageMoveToForegroundEvent(): Int =
        android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND

    @Suppress("DEPRECATION")
    private fun usageMoveToBackgroundEvent(): Int =
        android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND

    companion object {
        const val KEY_PACKAGE_NAME = "packageName"
        private const val LOOKBACK_MS = 30_000L
        private const val SESSION_STATE_IN_USE = "IN_USE"
    }
}
