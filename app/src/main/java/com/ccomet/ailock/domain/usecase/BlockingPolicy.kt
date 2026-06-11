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
        todayUsageMinutes: Int,
        hasActiveTemporaryAllowance: Boolean,
        hasExpiredActiveSession: Boolean = false,
        ignoredPackages: Set<String>,
        now: Long = System.currentTimeMillis(),
    ): BlockDecision {
        if (isIgnored(packageName, ignoredPackages)) return BlockDecision.Allow

        val config = lockedApps.firstOrNull { it.packageName == packageName }
            ?: return BlockDecision.Allow

        if (config.selectedDays.isNotEmpty() && today !in config.selectedDays) {
            return BlockDecision.Allow
        }

        if (hasExpiredActiveSession) {
            return BlockDecision.ShowIntervention(
                config = config,
                reason = "temporary allowance expired",
                timeLimitExceeded = true,
            )
        }

        val dailyLimit = config.dailyLimitMinutes
        if (dailyLimit != null && todayUsageMinutes >= dailyLimit) {
            return BlockDecision.ShowIntervention(
                config = config,
                reason = "daily hard limit exceeded",
                timeLimitExceeded = true,
            )
        }

        if (hasActiveTemporaryAllowance) return BlockDecision.Allow

        val lockUntilAt = config.lockUntilAt ?: 0L
        if (now < lockUntilAt) {
            return BlockDecision.ShowIntervention(
                config = config,
                reason = "manual lock timer active",
                timeLimitExceeded = false,
            )
        }

        return BlockDecision.Allow
    }
}
