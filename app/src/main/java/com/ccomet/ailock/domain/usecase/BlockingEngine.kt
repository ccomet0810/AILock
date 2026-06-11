package com.ccomet.ailock.domain.usecase

import android.content.Context
import android.content.Intent
import com.ccomet.ailock.data.repository.AILockRepository
import com.ccomet.ailock.domain.model.BlockDecision
import com.ccomet.ailock.util.AppListLoader
import com.ccomet.ailock.util.TimeUtils
import kotlinx.coroutines.flow.first

class BlockingEngine(
    private val context: Context,
    private val repository: AILockRepository,
    private val appListLoader: AppListLoader,
) {
    suspend fun evaluate(packageName: String): BlockDecision {
        val ignoredPackages = ignoredPackages()
        if (BlockingPolicy.isIgnored(packageName, ignoredPackages)) return BlockDecision.Allow

        val usageRecords = repository.usageRecords.first()
        val todayUsageMinutes = TimeUtils.todayRecords(usageRecords)
            .filter { it.packageName == packageName }
            .sumOf { it.durationMinutes }

        return BlockingPolicy.evaluate(
            packageName = packageName,
            lockedApps = repository.lockedApps.first(),
            today = TimeUtils.currentDayOfWeek(),
            todayUsageMinutes = todayUsageMinutes,
            hasActiveTemporaryAllowance = repository.hasActiveTemporaryAllowance(packageName),
            ignoredPackages = ignoredPackages,
        )
    }

    suspend fun recordOpen(packageName: String) {
        val appName = appListLoader.appName(packageName)
        repository.recordEvent(packageName, appName, com.ccomet.ailock.data.model.UsageEventType.OPEN)
    }

    private fun ignoredPackages(): Set<String> =
        setOf(context.packageName, "com.android.settings", "com.android.systemui") + homePackages()

    private fun homePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return context.packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }
}

