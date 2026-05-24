package com.ccomet.ailock.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.data.model.RestrictionType
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.service.UsageMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

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
            repository.backendBaseUrl.collect { url ->
                _uiState.update { it.copy(backendBaseUrl = url, backendBaseUrlInput = url) }
            }
        }
        viewModelScope.launch {
            repository.mockAiMode.collect { enabled ->
                _uiState.update { it.copy(mockAiMode = enabled) }
            }
        }
        viewModelScope.launch {
            repository.willPowerScore.collect { score ->
                _uiState.update { it.copy(willPowerScore = score) }
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

    fun nextOnboardingStep() {
        _uiState.update {
            val next = (it.onboardingStep + 1).coerceAtMost(4)
            it.copy(
                onboardingStep = next,
                isEditingProfile = next == 3,
                profileDraft = if (next == 3) it.profileDraft.takeUnless { draft -> draft.isBlank() } ?: it.userProfile else it.profileDraft,
            )
        }
    }

    fun previousOnboardingStep() {
        _uiState.update { it.copy(onboardingStep = (it.onboardingStep - 1).coerceAtLeast(0)) }
    }

    fun skipPermissionGateForDebug() {
        _uiState.update {
            it.copy(
                onboardingStep = 3,
                isEditingProfile = true,
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

    fun finishOnboarding() {
        viewModelScope.launch { repository.setOnboardingCompleted(true) }
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
                    selectedDays = config.selectedDays,
                    dailyLimitMinutes = config.dailyLimitMinutes ?: 30,
                    advancedDayLimits = config.advancedDayLimits,
                    isAdvancedSchedule = config.isAdvancedSchedule,
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
        _uiState.update { it.copy(draft = it.draft.copy(dailyLimitMinutes = minutes)) }
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
            _uiState.update { it.copy(statusMessage = "앱, 이유, 제한 방식을 먼저 채워주세요.") }
            return
        }
        viewModelScope.launch {
            repository.upsertLockedApp(draft.toConfig())
            _uiState.update { it.copy(statusMessage = "${draft.appName} 제한을 저장했어요.") }
            onSaved()
        }
    }

    fun deleteDraft(onDeleted: () -> Unit) {
        val id = _uiState.value.draft.id ?: return
        viewModelScope.launch {
            repository.deleteLockedApp(id)
            _uiState.update { it.copy(statusMessage = "제한 앱을 삭제했어요.") }
            onDeleted()
        }
    }

    fun updateBackendBaseUrlInput(url: String) {
        _uiState.update { it.copy(backendBaseUrlInput = url) }
    }

    fun saveBackendBaseUrl() {
        viewModelScope.launch {
            repository.setBackendBaseUrl(_uiState.value.backendBaseUrlInput)
            _uiState.update { it.copy(statusMessage = "백엔드 주소를 저장했어요.") }
        }
    }

    fun setMockAiMode(enabled: Boolean) {
        viewModelScope.launch { repository.setMockAiMode(enabled) }
    }

    fun startUsageMonitor() {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(context, Intent(context, UsageMonitorService::class.java))
        _uiState.update { it.copy(statusMessage = "사용 모니터 서비스를 시작했어요.") }
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
        getApplication<Application>().startActivity(intent)
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
