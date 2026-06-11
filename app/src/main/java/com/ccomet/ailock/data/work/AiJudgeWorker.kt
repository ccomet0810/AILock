package com.ccomet.ailock.data.work

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.data.model.JudgePostRequest
import com.ccomet.ailock.data.model.JudgePreRequest
import com.ccomet.ailock.data.model.PendingFinalDecision
import com.ccomet.ailock.util.TimeUtils
import kotlinx.coroutines.flow.first

class AiJudgeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()
        val container = AILockContainer.get(applicationContext)

        return when (action) {
            ACTION_PRE -> handlePre(container)
            ACTION_POST -> handlePost(container)
            ACTION_UPDATE -> Result.success()
            else -> Result.failure()
        }
    }

    private suspend fun handlePre(container: AILockContainer): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        val appName = inputData.getString(KEY_APP_NAME) ?: return Result.failure()
        val preInput = inputData.getString(KEY_PRE_INPUT) ?: return Result.failure()
        val plannedMinutes = inputData.getInt(KEY_PLANNED_MINUTES, 0)
        if (plannedMinutes <= 0) return Result.failure()

        val current = container.activeUseSessionRepository.get(packageName) ?: return Result.success()
        if (current.sessionId.isNotBlank()) return Result.success()
        val config = container.ailockRepository.lockedApps.first()
            .firstOrNull { it.packageName == packageName }
        val dailyLimit = config?.let { dailyLimitMinutes(it) } ?: DEFAULT_DAILY_LIMIT_MINUTES
        val todayUsage = todayUsageMinutes(packageName)

        val pre = container.ollamaDecisionRepository.judgePre(
            JudgePreRequest(
                appName = appName,
                preInput = preInput,
                requestUseTime = plannedMinutes,
                todayAppUsageMinutes = todayUsage,
                dailyLimitMinutes = dailyLimit,
            ),
        )

        val latest = container.activeUseSessionRepository.get(packageName)
        if (latest != null && latest.sessionId.isBlank()) {
            container.activeUseSessionRepository.save(
                latest.copy(
                    sessionId = pre.sessionId,
                    backendDeviceId = pre.backendDeviceId,
                ),
            )
        }
        return Result.success()
    }

    private suspend fun handlePost(container: AILockContainer): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        val postInput = inputData.getString(KEY_POST_INPUT) ?: return Result.failure()
        val session = container.activeUseSessionRepository.get(packageName) ?: return Result.success()
        val config = container.ailockRepository.lockedApps.first()
            .firstOrNull { it.packageName == packageName }
        val dailyLimit = config?.let { dailyLimitMinutes(it) } ?: DEFAULT_DAILY_LIMIT_MINUTES
        val todayUsage = todayUsageMinutes(packageName)

        val post = container.ollamaDecisionRepository.judgePost(
            JudgePostRequest(
                sessionId = session.sessionId,
                appName = session.appName,
                previousReason = session.preInput,
                postInput = postInput,
                requestCount = 1,
                todayAppUsageMinutes = todayUsage,
                dailyLimitMinutes = dailyLimit,
                backendDeviceId = session.backendDeviceId,
            ),
        )

        container.pendingFinalDecisionRepository.save(
            packageName,
            PendingFinalDecision(
                sessionId = session.sessionId.ifBlank { "ollama-session-local" },
                status = post.status,
                allowedTime = post.allowedTime,
                supportMessage = post.supportMessage,
                backendDeviceId = session.backendDeviceId,
            ),
        )
        return Result.success()
    }

    private fun dailyLimitMinutes(config: com.ccomet.ailock.data.model.LockedAppConfig): Int {
        val today = TimeUtils.currentDayOfWeek()
        return (config.advancedDayLimits[today] ?: config.dailyLimitMinutes ?: DEFAULT_DAILY_LIMIT_MINUTES)
            .coerceAtLeast(1)
    }

    private fun todayUsageMinutes(packageName: String): Int {
        val manager = applicationContext.getSystemService(UsageStatsManager::class.java)
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            TimeUtils.todayStartMillis(),
            System.currentTimeMillis(),
        )
        val total = stats
            ?.filter { it.packageName == packageName }
            ?.sumOf { it.totalTimeInForeground }
            ?: 0L
        return (total / 60_000L).toInt()
    }

    companion object {
        const val ACTION_PRE = "ACTION_PRE"
        const val ACTION_POST = "ACTION_POST"
        const val ACTION_UPDATE = "ACTION_UPDATE"

        const val KEY_ACTION = "action"
        const val KEY_PACKAGE_NAME = "packageName"
        const val KEY_APP_NAME = "appName"
        const val KEY_PRE_INPUT = "preInput"
        const val KEY_PLANNED_MINUTES = "plannedMinutes"
        const val KEY_POST_INPUT = "postInput"
        const val KEY_SESSION_ID = "sessionId"
        const val KEY_TOTAL_USE_TIME = "totalUseTime"

        private const val DEFAULT_DAILY_LIMIT_MINUTES = 120
    }
}
