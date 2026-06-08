package com.ccomet.ailock.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.data.model.ActiveUseSession
import com.ccomet.ailock.service.AILockAccessibilityService
import com.ccomet.ailock.service.AILockOverlayController
import kotlinx.coroutines.flow.first

class OverlayNudgeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        val nudgeType = inputData.getString(KEY_NUDGE_TYPE) ?: return Result.failure()

        val container = AILockContainer.get(applicationContext)
        val session = container.activeUseSessionRepository.get(packageName) ?: return Result.success()

        return when (nudgeType) {
            NUDGE_TIME_EXPIRED -> {
                if (session.state != SESSION_STATE_IN_USE) {
                    Result.success()
                } else if (System.currentTimeMillis() < session.expectedEndAt) {
                    Result.retry()
                } else if (!isTargetForeground(packageName)) {
                    Result.success()
                } else {
                    showOverlay(container, packageName, session)
                    Result.success()
                }
            }

            NUDGE_FINAL_DECISION -> {
                if (session.state != SESSION_STATE_WAITING_FINAL_DECISION) {
                    Result.success()
                } else if (!isTargetForeground(packageName)) {
                    Result.success()
                } else {
                    showOverlay(container, packageName, session)
                    Result.success()
                }
            }

            else -> Result.failure()
        }
    }

    private suspend fun showOverlay(container: AILockContainer, packageName: String, session: ActiveUseSession) {
        val config = container.ailockRepository.lockedApps.first()
            .firstOrNull { it.packageName == packageName }
            ?: return
        AILockOverlayController.show(
            context = applicationContext,
            config = config,
            timeLimitExceeded = true,
            initialSession = session,
            initialPending = container.pendingFinalDecisionRepository.get(packageName),
        )
    }

    private fun isTargetForeground(packageName: String): Boolean =
        AILockAccessibilityService.currentForegroundPackage() == packageName

    companion object {
        const val KEY_PACKAGE_NAME = "packageName"
        const val KEY_NUDGE_TYPE = "nudgeType"

        const val NUDGE_TIME_EXPIRED = "NUDGE_TIME_EXPIRED"
        const val NUDGE_FINAL_DECISION = "NUDGE_FINAL_DECISION"

        private const val SESSION_STATE_IN_USE = "IN_USE"
        private const val SESSION_STATE_WAITING_FINAL_DECISION = "WAITING_FINAL_DECISION"
    }
}
