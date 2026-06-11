package com.ccomet.ailock.ui

import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.PermissionState
import com.ccomet.ailock.data.model.RestrictionType
import com.ccomet.ailock.data.model.UsageRecord
import com.ccomet.ailock.data.model.UserProfile
import java.time.DayOfWeek

data class AILockUiState(
    val onboardingCompleted: Boolean = false,
    val onboardingStep: Int = 0,
    val userProfile: UserProfile = UserProfile(),
    val profileDraft: UserProfile = UserProfile(),
    val isEditingProfile: Boolean = false,
    val permissions: PermissionState = PermissionState(),
    val lockedApps: List<LockedAppConfig> = emptyList(),
    val usageRecords: List<UsageRecord> = emptyList(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val appQuery: String = "",
    val onboardingSelectedPackages: Set<String> = emptySet(),
    val onboardingSelectedApps: List<InstalledAppInfo> = emptyList(),
    val onboardingDailyLimitMinutes: Int = 120,
    val draft: LockedAppDraft = LockedAppDraft(),
    val willPowerScore: Int = 80,
    val backendBaseUrl: String = "",
    val statusMessage: String? = null,
)

data class LockedAppDraft(
    val id: Long? = null,
    val packageName: String = "",
    val appName: String = "",
    val lockReasonPreset: String? = null,
    val lockReasonCustom: String = "",
    val restrictionType: RestrictionType = RestrictionType.IMMEDIATE_LOCK,
    val selectedDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val dailyLimitMinutes: Int = 120,
    val advancedDayLimits: Map<DayOfWeek, Int> = emptyMap(),
    val isAdvancedSchedule: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val lockReasonFinal: String
        get() = listOfNotNull(
            lockReasonPreset?.takeIf { it != DIRECT_INPUT },
            lockReasonCustom.takeIf { it.isNotBlank() },
        ).joinToString(" / ").trim()

    val isValid: Boolean
        get() = packageName.isNotBlank() &&
            appName.isNotBlank() &&
            dailyLimitMinutes >= 0

    fun toConfig(): LockedAppConfig = LockedAppConfig(
        id = id ?: System.currentTimeMillis(),
        packageName = packageName,
        appName = appName,
        lockReasonPreset = lockReasonPreset,
        lockReasonCustom = lockReasonCustom,
        lockReasonFinal = lockReasonFinal,
        restrictionType = RestrictionType.IMMEDIATE_LOCK,
        selectedDays = DayOfWeek.entries.toSet(),
        dailyLimitMinutes = dailyLimitMinutes,
        advancedDayLimits = emptyMap(),
        isAdvancedSchedule = false,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
    )

    companion object {
        const val DIRECT_INPUT = "직접 입력"
    }
}

