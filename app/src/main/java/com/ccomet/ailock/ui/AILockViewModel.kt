package com.ccomet.ailock.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.data.model.RestrictionType
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.data.work.SessionWorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

private const val LAST_ONBOARDING_STEP = 6
private const val PROFILE_INPUT_STEP = 3
private const val MIN_TIMER_MINUTES = 10

class AILockViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AILockContainer.get(application)
    private val repository = container.ailockRepository
    private val permissionRepository = container.permissionRepository
    private val appListLoader = container.appListLoader

    private val _uiState = MutableStateFlow(AILockUiState())
    val uiState: StateFlow<AILockUiState> = _uiState

    init {
        container.notificationHelper.ensureChannels()
        refreshPermissions()

        viewModelScope.launch {
            repository.onboardingCompleted.collect { completed ->
                _uiState.update { it.copy(onboardingCompleted = completed) }
            }
        }
        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                _uiState.update {
                    it.copy(
                        userProfile = profile,
                        profileDraft = if (it.isEditingProfile) it.profileDraft else profile,
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.lockedApps.collect { apps ->
                _uiState.update { it.copy(lockedApps = apps) }
                reloadInstalledApps()
            }
        }
        viewModelScope.launch {
            repository.usageRecords.collect { records ->
                _uiState.update { it.copy(usageRecords = records) }
            }
        }
        viewModelScope.launch {
            repository.willPowerScore.collect { score ->
                _uiState.update { it.copy(willPowerScore = score) }
            }
        }
        viewModelScope.launch {
            repository.backendBaseUrl.collect { url ->
                _uiState.update { it.copy(backendBaseUrl = url) }
            }
        }
        reloadInstalledApps()
    }

    fun refreshPermissions() {
        _uiState.update { it.copy(permissions = permissionRepository.currentState()) }
    }

    fun openUsageAccessSettings() = openIntent(permissionRepository.usageAccessIntent())
    fun openOverlaySettings() = openIntent(permissionRepository.overlayIntent())
    fun openAccessibilitySettings() = openIntent(permissionRepository.accessibilityIntent())
    fun openNotificationSettings() = openIntent(permissionRepository.notificationSettingsIntent())
    fun openBatteryOptimizationSettings() = openIntent(permissionRepository.batteryOptimizationIntent())

    fun openNextMissingPermission() {
        refreshPermissions()
        val permissions = _uiState.value.permissions
        when {
            !permissions.hasUsageAccess -> openUsageAccessSettings()
            !permissions.isAccessibilityEnabled -> openAccessibilitySettings()
            !permissions.canDrawOverlays -> openOverlaySettings()
            !permissions.isIgnoringBatteryOptimizations -> openBatteryOptimizationSettings()
            else -> finishOnboarding()
        }
    }

    fun nextOnboardingStep() {
        _uiState.update {
            val next = (it.onboardingStep + 1).coerceAtMost(LAST_ONBOARDING_STEP)
            it.copy(
                onboardingStep = next,
                isEditingProfile = next == PROFILE_INPUT_STEP,
                profileDraft = if (next == PROFILE_INPUT_STEP) {
                    it.profileDraft.takeUnless { draft -> draft.isBlank() } ?: it.userProfile
                } else {
                    it.profileDraft
                },
            )
        }
    }

    fun previousOnboardingStep() {
        _uiState.update { it.copy(onboardingStep = (it.onboardingStep - 1).coerceAtLeast(0)) }
    }

    fun skipPermissionGateForDebug() {
        _uiState.update {
            it.copy(
                onboardingStep = LAST_ONBOARDING_STEP,
                isEditingProfile = false,
                profileDraft = it.profileDraft.takeUnless { draft -> draft.isBlank() } ?: it.userProfile,
                statusMessage = "디버그 모드로 권한 없이 계속합니다.",
            )
        }
    }

    fun startProfileEditing() {
        _uiState.update { it.copy(profileDraft = it.userProfile, isEditingProfile = true) }
    }

    fun updateProfileDraft(profile: UserProfile) {
        _uiState.update { it.copy(profileDraft = profile, isEditingProfile = true) }
    }

    fun saveProfileAndContinue() {
        val profile = _uiState.value.profileDraft
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _uiState.update { it.copy(userProfile = profile, isEditingProfile = false) }
            nextOnboardingStep()
        }
    }

    fun saveProfileAndFinish() {
        val profile = _uiState.value.profileDraft
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _uiState.update { it.copy(userProfile = profile, profileDraft = profile, isEditingProfile = false) }
            repository.setOnboardingCompleted(true)
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch { repository.setOnboardingCompleted(true) }
    }

    fun toggleOnboardingApp(packageName: String) {
        val app = _uiState.value.installedApps.firstOrNull { it.packageName == packageName } ?: return
        _uiState.update {
            val current = it.onboardingSelectedPackages
            if (packageName in current) {
                it.copy(
                    onboardingSelectedPackages = current - packageName,
                    onboardingSelectedApps = it.onboardingSelectedApps.filterNot { selected -> selected.packageName == packageName },
                    onboardingAppDailyLimits = it.onboardingAppDailyLimits - packageName,
                )
            } else {
                it.copy(
                    onboardingSelectedPackages = current + packageName,
                    onboardingSelectedApps = it.onboardingSelectedApps + app,
                    onboardingAppDailyLimits = it.onboardingAppDailyLimits + (packageName to 120),
                )
            }
        }
    }

    fun saveOnboardingAppsAndContinue() {
        val snapshot = _uiState.value
        val selectedApps = snapshot.onboardingSelectedApps
        if (selectedApps.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "관리할 앱을 하나 이상 골라주세요.") }
            return
        }
        viewModelScope.launch {
            selectedApps.forEachIndexed { index, app ->
                repository.upsertLockedApp(
                    LockedAppDraft(
                        id = System.currentTimeMillis() + index,
                        packageName = app.packageName,
                        appName = app.appName,
                        dailyLimitMinutes = snapshot.onboardingAppDailyLimits[app.packageName] ?: 120,
                    ).toConfig(),
                )
            }
            _uiState.update {
                it.copy(
                    onboardingSelectedPackages = emptySet(),
                    onboardingSelectedApps = emptyList(),
                    onboardingAppDailyLimits = emptyMap(),
                    appQuery = "",
                    statusMessage = "${selectedApps.joinToString(", ") { app -> app.appName }} 제한을 저장했어요.",
                )
            }
            nextOnboardingStep()
        }
    }

    fun updateOnboardingDailyLimit(minutes: Int) {
        _uiState.update { it.copy(onboardingDailyLimitMinutes = minutes.coerceIn(MIN_TIMER_MINUTES, 23 * 60 + 59)) }
    }

    fun updateOnboardingAppDailyLimit(packageName: String, minutes: Int) {
        _uiState.update {
            it.copy(
                onboardingAppDailyLimits = it.onboardingAppDailyLimits + (packageName to minutes.coerceIn(MIN_TIMER_MINUTES, 23 * 60 + 59)),
            )
        }
    }

    fun restartOnboarding() {
        _uiState.update {
            it.copy(
                onboardingStep = 0,
                profileDraft = it.userProfile,
                isEditingProfile = false,
                statusMessage = "온보딩을 다시 볼 수 있어요.",
            )
        }
        viewModelScope.launch { repository.setOnboardingCompleted(false) }
    }

    fun saveProfile() {
        val profile = _uiState.value.profileDraft
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _uiState.update {
                it.copy(
                    userProfile = profile,
                    profileDraft = profile,
                    isEditingProfile = false,
                    statusMessage = "개인정보를 저장했어요.",
                )
            }
        }
    }

    fun saveBackendBaseUrl(url: String) {
        viewModelScope.launch {
            repository.saveBackendBaseUrl(url)
            _uiState.update { it.copy(statusMessage = "서버 주소를 저장했어요.") }
        }
    }

    fun beginAddRestriction() {
        _uiState.update { it.copy(draft = LockedAppDraft(), appQuery = "") }
        reloadInstalledApps()
    }

    fun beginEditRestriction(id: Long) {
        val config = _uiState.value.lockedApps.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                draft = LockedAppDraft(
                    id = config.id,
                    packageName = config.packageName,
                    appName = config.appName,
                    lockReasonPreset = config.lockReasonPreset,
                    lockReasonCustom = config.lockReasonCustom,
                    restrictionType = config.restrictionType,
                    selectedDays = DayOfWeek.entries.toSet(),
                    dailyLimitMinutes = config.dailyLimitMinutes ?: 120,
                    lockTimerMinutes = 60,
                    lockUntilAt = config.lockUntilAt,
                    advancedDayLimits = emptyMap(),
                    isAdvancedSchedule = false,
                    createdAt = config.createdAt,
                ),
            )
        }
    }

    fun updateAppQuery(query: String) {
        _uiState.update { it.copy(appQuery = query) }
        reloadInstalledApps()
    }

    fun selectApp(packageName: String) {
        val app = _uiState.value.installedApps.firstOrNull { it.packageName == packageName } ?: return
        _uiState.update {
            it.copy(draft = it.draft.copy(packageName = app.packageName, appName = app.appName))
        }
    }

    fun updateReasonPreset(preset: String) {
        _uiState.update { it.copy(draft = it.draft.copy(lockReasonPreset = preset)) }
    }

    fun updateReasonCustom(reason: String) {
        _uiState.update { it.copy(draft = it.draft.copy(lockReasonCustom = reason)) }
    }

    fun updateRestrictionType(type: RestrictionType) {
        _uiState.update { it.copy(draft = it.draft.copy(restrictionType = type)) }
    }

    fun toggleDay(day: DayOfWeek) {
        _uiState.update {
            val current = it.draft.selectedDays
            it.copy(draft = it.draft.copy(selectedDays = if (day in current) current - day else current + day))
        }
    }

    fun updateDailyLimit(minutes: Int) {
        _uiState.update { it.copy(draft = it.draft.copy(dailyLimitMinutes = minutes.coerceIn(MIN_TIMER_MINUTES, 23 * 60 + 59))) }
    }

    fun updateLockTimer(minutes: Int) {
        _uiState.update { it.copy(draft = it.draft.copy(lockTimerMinutes = minutes.coerceIn(MIN_TIMER_MINUTES, 23 * 60 + 59))) }
    }

    fun startDraftLockTimer(onStarted: () -> Unit) {
        val draft = _uiState.value.draft
        val id = draft.id ?: return
        val config = _uiState.value.lockedApps.firstOrNull { it.id == id } ?: return
        val lockStartedAt = System.currentTimeMillis()
        val lockUntilAt = lockStartedAt + draft.lockTimerMinutes * 60_000L
        viewModelScope.launch {
            repository.upsertLockedApp(
                config.copy(
                    lockUntilAt = lockUntilAt,
                    lockStartedAt = lockStartedAt,
                    lockDurationMinutes = draft.lockTimerMinutes,
                ),
            )
            _uiState.update {
                it.copy(
                    draft = it.draft.copy(lockUntilAt = lockUntilAt),
                    statusMessage = "${draft.appName} 잠금 타이머를 시작했어요.",
                )
            }
            onStarted()
        }
    }

    fun toggleAdvancedSchedule() {
        _uiState.update {
            val draft = it.draft
            val nextAdvanced = !draft.isAdvancedSchedule
            val defaultMap = DayOfWeek.entries.associateWith { draft.dailyLimitMinutes }
            it.copy(
                draft = draft.copy(
                    isAdvancedSchedule = nextAdvanced,
                    advancedDayLimits = if (nextAdvanced) draft.advancedDayLimits.ifEmpty { defaultMap } else emptyMap(),
                ),
            )
        }
    }

    fun updateAdvancedLimit(day: DayOfWeek, minutes: Int) {
        _uiState.update {
            it.copy(draft = it.draft.copy(advancedDayLimits = it.draft.advancedDayLimits + (day to minutes)))
        }
    }

    fun saveDraft(onSaved: () -> Unit) {
        val draft = _uiState.value.draft
        if (!draft.isValid) {
            _uiState.update { it.copy(statusMessage = "앱과 판단 기준을 먼저 채워주세요.") }
            return
        }
        viewModelScope.launch {
            repository.upsertLockedApp(draft.toConfig())
            _uiState.update { it.copy(statusMessage = "${draft.appName} 제한을 저장했어요.") }
            onSaved()
        }
    }

    fun deleteDraft(onDeleted: () -> Unit) {
        val draft = _uiState.value.draft
        val id = draft.id ?: return
        viewModelScope.launch {
            repository.deleteLockedApp(id)
            SessionWorkScheduler.cancelAllForPackage(getApplication(), draft.packageName)
            container.activeUseSessionRepository.clear(draft.packageName)
            container.pendingFinalDecisionRepository.clear(draft.packageName)
            _uiState.update { it.copy(statusMessage = "제한 앱을 삭제했어요.") }
            onDeleted()
        }
    }

    fun deleteLockedApp(id: Long, onDeleted: () -> Unit = {}) {
        val config = _uiState.value.lockedApps.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.deleteLockedApp(id)
            SessionWorkScheduler.cancelAllForPackage(getApplication(), config.packageName)
            container.activeUseSessionRepository.clear(config.packageName)
            container.pendingFinalDecisionRepository.clear(config.packageName)
            _uiState.update { it.copy(statusMessage = "${config.appName} 제한을 삭제했어요.") }
            onDeleted()
        }
    }

    private fun reloadInstalledApps() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            val apps = withContext(Dispatchers.Default) {
                appListLoader.load(
                    query = snapshot.appQuery,
                    lockedPackages = snapshot.lockedApps.map { it.packageName }.toSet(),
                )
            }
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    private fun openIntent(intent: Intent) {
        runCatching {
            getApplication<Application>().startActivity(intent)
        }.onFailure {
            _uiState.update { state -> state.copy(statusMessage = "설정 화면을 열 수 없어요. 기기 설정에서 권한을 직접 확인해주세요.") }
        }
    }

    private fun UserProfile.isBlank(): Boolean =
        name.isBlank() && age == null && gender.isBlank() && job.isBlank()

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AILockViewModel(application) as T
        }
    }
}

