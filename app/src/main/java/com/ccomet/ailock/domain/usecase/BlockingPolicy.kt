package com.ccomet.ailock.domain.usecase

import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.domain.model.BlockDecision
import java.time.DayOfWeek

object BlockingPolicy {
    fun isIgnored(packageName: String, ignoredPackages: Set<String>): Boolean =
        packageName.isBlank() || packageName in ignoredPackages

    fun evaluate(
        packageName: String,
        lockedApps: List<LockedAppConfig>,
        today: DayOfWeek,
        hasActiveTemporaryAllowance: Boolean,
        ignoredPackages: Set<String>,
    ): BlockDecision {
        if (isIgnored(packageName, ignoredPackages)) return BlockDecision.Allow
        if (hasActiveTemporaryAllowance) return BlockDecision.Allow

        val config = lockedApps.firstOrNull { it.packageName == packageName }
            ?: return BlockDecision.Allow

        if (config.selectedDays.isNotEmpty() && today !in config.selectedDays) {
            return BlockDecision.Allow
        }

        return BlockDecision.ShowIntervention(
            config = config,
            reason = "ai unlock judgment",
        )
    }
}
