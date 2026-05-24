package com.ccomet.ailock.data.model

import android.graphics.drawable.Drawable
import java.time.DayOfWeek

data class UserProfile(
    val name: String = "",
    val age: Int? = null,
    val gender: String = "",
    val job: String = "",
)

data class PermissionState(
    val hasUsageAccess: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val hasNotificationPermission: Boolean = false,
) {
    val allRequiredGranted: Boolean
        get() = hasUsageAccess && canDrawOverlays && isAccessibilityEnabled && hasNotificationPermission
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val category: AppCategory = AppCategory.OTHER,
    val icon: Drawable? = null,
    val isLocked: Boolean = false,
)

enum class AppCategory(val label: String) {
    SNS("SNS"),
    VIDEO("영상/스트리밍"),
    GAME("게임"),
    WEBTOON("웹툰/웹소설"),
    SHOPPING("쇼핑"),
    BROWSER("브라우저"),
    MESSENGER("메신저"),
    OTHER("기타"),
}

enum class RestrictionType(val label: String) {
    IMMEDIATE_LOCK("지금 바로 잠금"),
    TIME_LIMIT("사용 시간 제한"),
}

data class LockedAppConfig(
    val id: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val appName: String = "",
    val iconUri: String? = null,
    val lockReasonPreset: String? = null,
    val lockReasonCustom: String = "",
    val lockReasonFinal: String = "",
    val restrictionType: RestrictionType = RestrictionType.IMMEDIATE_LOCK,
    val selectedDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val dailyLimitMinutes: Int? = null,
    val advancedDayLimits: Map<DayOfWeek, Int> = emptyMap(),
    val isAdvancedSchedule: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class UsageRecord(
    val id: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val appName: String = "",
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val durationMinutes: Int = 0,
    val eventType: UsageEventType = UsageEventType.OPEN,
    val aiStatus: String? = null,
    val aiAllowedTime: Int? = null,
    val userInput: String? = null,
    val lockReason: String? = null,
)

enum class UsageEventType {
    OPEN,
    CLOSE,
    NOTIFICATION_WARNING,
    OVERLAY_SHOWN,
    AI_REQUEST,
    AI_ALLOWED,
    AI_DENIED,
    SELF_STOP,
    PLEDGE,
    FORCE_HOME,
}

data class CurrentStats(
    val willPowerScore: Int = 80,
    val todayOpenAppCount: Int = 0,
    val accumUseApp: Int = 0,
)

data class AiDecisionRequest(
    val appName: String,
    val userInput: String,
    val lockReason: String,
    val currentStats: CurrentStats,
)

data class AiDecisionResponse(
    val status: AiDecisionStatus = AiDecisionStatus.WARNING,
    val text: String = "좋아, 딱 10분만 보고 마무리하자.",
    val allowedTime: Int = 10,
    val source: String? = null,
)

enum class AiDecisionStatus {
    OPTIMAL,
    WARNING,
    OVERUSE,
    CRITICAL,
    FAIL,
}

enum class PandaEmotion {
    DEFAULT,
    HAPPY,
    ENCOURAGING,
    THINKING,
    SUSPICIOUS,
    ANGRY,
    SAD,
    DISAPPOINTED,
}
