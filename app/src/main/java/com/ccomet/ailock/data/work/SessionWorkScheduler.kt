package com.ccomet.ailock.data.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object SessionWorkScheduler {
    private const val FINAL_DELAY_DEFAULT_MS = 60_000L

    fun enqueuePreDecision(
        context: Context,
        packageName: String,
        appName: String,
        preInput: String,
        plannedMinutes: Int,
    ) {
        val request = OneTimeWorkRequestBuilder<AiJudgeWorker>()
            .setInputData(
                workDataOf(
                    AiJudgeWorker.KEY_ACTION to AiJudgeWorker.ACTION_PRE,
                    AiJudgeWorker.KEY_PACKAGE_NAME to packageName,
                    AiJudgeWorker.KEY_APP_NAME to appName,
                    AiJudgeWorker.KEY_PRE_INPUT to preInput,
                    AiJudgeWorker.KEY_PLANNED_MINUTES to plannedMinutes,
                ),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniquePreName(packageName),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun enqueuePostDecision(
        context: Context,
        packageName: String,
        postInput: String,
    ) {
        val request = OneTimeWorkRequestBuilder<AiJudgeWorker>()
            .setInputData(
                workDataOf(
                    AiJudgeWorker.KEY_ACTION to AiJudgeWorker.ACTION_POST,
                    AiJudgeWorker.KEY_PACKAGE_NAME to packageName,
                    AiJudgeWorker.KEY_POST_INPUT to postInput,
                ),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniquePostName(packageName),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun enqueueSessionUpdate(
        context: Context,
        sessionId: String,
        totalUseTime: Int,
    ) {
        if (sessionId.isBlank()) return

        val request = OneTimeWorkRequestBuilder<AiJudgeWorker>()
            .setInputData(
                workDataOf(
                    AiJudgeWorker.KEY_ACTION to AiJudgeWorker.ACTION_UPDATE,
                    AiJudgeWorker.KEY_SESSION_ID to sessionId,
                    AiJudgeWorker.KEY_TOTAL_USE_TIME to totalUseTime,
                ),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueUpdateName(sessionId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleTimeExpiredNudge(
        context: Context,
        packageName: String,
        delayMs: Long,
    ) {
        scheduleNudge(
            context = context,
            packageName = packageName,
            nudgeType = OverlayNudgeWorker.NUDGE_TIME_EXPIRED,
            delayMs = delayMs,
            uniqueName = uniqueTimeNudgeName(packageName),
        )
    }

    fun scheduleFinalDecisionNudge(
        context: Context,
        packageName: String,
        delayMs: Long = FINAL_DELAY_DEFAULT_MS,
    ) {
        scheduleNudge(
            context = context,
            packageName = packageName,
            nudgeType = OverlayNudgeWorker.NUDGE_FINAL_DECISION,
            delayMs = delayMs,
            uniqueName = uniqueFinalNudgeName(packageName),
        )
    }

    fun scheduleForegroundExitUpdate(
        context: Context,
        packageName: String,
        delayMs: Long = 10_000L,
    ) {
        val request = OneTimeWorkRequestBuilder<SessionExitWorker>()
            .setInputData(
                workDataOf(
                    SessionExitWorker.KEY_PACKAGE_NAME to packageName,
                ),
            )
            .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueExitName(packageName),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelForegroundExitUpdate(context: Context, packageName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueExitName(packageName))
    }

    fun cancelAllForPackage(context: Context, packageName: String) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(uniquePreName(packageName))
        wm.cancelUniqueWork(uniquePostName(packageName))
        wm.cancelUniqueWork(uniqueTimeNudgeName(packageName))
        wm.cancelUniqueWork(uniqueFinalNudgeName(packageName))
        wm.cancelUniqueWork(uniqueExitName(packageName))
    }

    private fun scheduleNudge(
        context: Context,
        packageName: String,
        nudgeType: String,
        delayMs: Long,
        uniqueName: String,
    ) {
        val request = OneTimeWorkRequestBuilder<OverlayNudgeWorker>()
            .setInputData(
                workDataOf(
                    OverlayNudgeWorker.KEY_PACKAGE_NAME to packageName,
                    OverlayNudgeWorker.KEY_NUDGE_TYPE to nudgeType,
                ),
            )
            .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun uniquePreName(packageName: String): String = "ai_pre_$packageName"
    private fun uniquePostName(packageName: String): String = "ai_post_$packageName"
    private fun uniqueUpdateName(sessionId: String): String = "ai_update_$sessionId"
    private fun uniqueTimeNudgeName(packageName: String): String = "nudge_time_$packageName"
    private fun uniqueFinalNudgeName(packageName: String): String = "nudge_final_$packageName"
    private fun uniqueExitName(packageName: String): String = "session_exit_$packageName"
}
