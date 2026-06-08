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
        ignoreReason(packageName)?.let { return BlockDecision.Allow }
        if (repository.hasActiveTemporaryAllowance(packageName)) return BlockDecision.Allow

        val config = repository.lockedApps.first().firstOrNull { it.packageName == packageName }
            ?: return BlockDecision.Allow

        val today = TimeUtils.currentDayOfWeek()
        if (config.selectedDays.isNotEmpty() && today !in config.selectedDays) {
            return BlockDecision.Allow
        }

        return BlockDecision.ShowIntervention(
            config = config,
            reason = "ai unlock judgment",
        )
    }

    suspend fun recordOpen(packageName: String) {
        val appName = appListLoader.appName(packageName)
        repository.recordEvent(packageName, appName, com.ccomet.ailock.data.model.UsageEventType.OPEN)
    }

    private fun ignoreReason(packageName: String): String? {
        if (packageName.isBlank()) return "blank package"
        if (packageName == context.packageName) return "own app"
        if (packageName == "com.android.settings") return "settings"
        if (packageName == "com.android.systemui") return "system ui"
        if (packageName in homePackages()) return "home launcher"
        return null
    }

    private fun homePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return context.packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }
}

