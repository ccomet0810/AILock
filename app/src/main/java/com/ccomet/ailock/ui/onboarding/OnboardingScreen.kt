package com.ccomet.ailock.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PermissionCards
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.PandaOrange

private const val LastOnboardingStep = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: AILockUiState,
    onOpenNextPermission: () -> Unit,
    onUsagePermission: () -> Unit,
    onOverlayPermission: () -> Unit,
    onAccessibilityPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onBatteryPermission: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onProfileChange: (UserProfile) -> Unit,
    onAppQuery: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSaveAppsAndContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    val step = uiState.onboardingStep.coerceIn(0, LastOnboardingStep)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AILock", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (step > 0) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onFinish) {
                        Text("스킵", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            if (step == 0 || step == LastOnboardingStep) {
                CenteredPandaStep(
                    text = if (step == 0) {
                        "안녕! 나는 네 앱 사용 약속을 지켜줄 레서판다야."
                    } else {
                        readyMessage(uiState)
                    },
                    emotion = if (step == 0) PandaEmotion.HAPPY else PandaEmotion.ENCOURAGING,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 140.dp)
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StepIndicator(currentStep = step + 1, totalSteps = LastOnboardingStep + 1)
                    when (step) {
                        1 -> PermissionsStep(
                            uiState = uiState,
                            onUsagePermission = onUsagePermission,
                            onOverlayPermission = onOverlayPermission,
                            onAccessibilityPermission = onAccessibilityPermission,
                            onNotificationPermission = onNotificationPermission,
                            onBatteryPermission = onBatteryPermission,
                        )
                        2 -> ProfileStep(
                            profile = uiState.profileDraft,
                            onProfileChange = onProfileChange,
                        )
                        3 -> AppsStep(
                            uiState = uiState,
                            onQuery = onAppQuery,
                            onToggleApp = onToggleApp,
                        )
                    }
                }
            }
            OnboardingFloatingBottomAction(
                step = step,
                uiState = uiState,
                onNext = onNext,
                onOpenNextPermission = onOpenNextPermission,
                onSaveAppsAndContinue = onSaveAppsAndContinue,
                onFinish = onFinish,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CenteredPandaStep(text: String, emotion: PandaEmotion, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PandaSpeechBubble(text = text, emotion = emotion)
    }
}

@Composable
private fun PermissionsStep(
    uiState: AILockUiState,
    onUsagePermission: () -> Unit,
    onOverlayPermission: () -> Unit,
    onAccessibilityPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onBatteryPermission: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        PandaSpeechBubble(
            text = "먼저 앱 사용을 감지하고 필요한 순간에 말을 걸 수 있게 권한을 확인할게.",
            emotion = PandaEmotion.THINKING,
        )
        PermissionCards(
            permissionState = uiState.permissions,
            onUsage = onUsagePermission,
            onOverlay = onOverlayPermission,
            onAccessibility = onAccessibilityPermission,
            onNotification = onNotificationPermission,
            onBattery = onBatteryPermission,
        )
    }
}

@Composable
private fun ProfileStep(profile: UserProfile, onProfileChange: (UserProfile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        PandaSpeechBubble("기본 정보를 알려주면 더 자연스럽게 도와줄게.", emotion = PandaEmotion.THINKING)
        OutlinedTextField(
            value = profile.name,
            onValueChange = { onProfileChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("이름") },
            singleLine = true,
        )
        OutlinedTextField(
            value = profile.age?.toString().orEmpty(),
            onValueChange = { onProfileChange(profile.copy(age = it.toIntOrNull())) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("나이") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = profile.gender,
                onValueChange = { onProfileChange(profile.copy(gender = it)) },
                modifier = Modifier.weight(1f),
                label = { Text("성별") },
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.job,
                onValueChange = { onProfileChange(profile.copy(job = it)) },
                modifier = Modifier.weight(1f),
                label = { Text("직업") },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun AppsStep(
    uiState: AILockUiState,
    onQuery: (String) -> Unit,
    onToggleApp: (String) -> Unit,
) {
    val selectedApps = uiState.onboardingSelectedApps
    val selectedNames = selectedApps.joinToString(", ") { it.appName }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        PandaSpeechBubble(
            text = if (selectedApps.isEmpty()) {
                "처음 관리할 앱을 골라줘."
            } else {
                "선택한 ${selectedNames}의 하루 사용 시간을 2시간으로 설정할게."
            },
            emotion = PandaEmotion.DEFAULT,
        )
        OutlinedTextField(
            value = uiState.appQuery,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            shape = AILockShape.control,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PandaOrange,
                unfocusedBorderColor = AppBorder,
                focusedContainerColor = AppSurface,
                unfocusedContainerColor = AppSurface,
            ),
            label = { Text("앱 이름 검색") },
            singleLine = true,
        )
        AppSelectionList(
            apps = uiState.installedApps,
            selectedPackages = uiState.onboardingSelectedPackages,
            onToggleApp = onToggleApp,
        )
    }
}

@Composable
private fun AppSelectionList(
    apps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
    onToggleApp: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Column {
            apps.forEachIndexed { index, app ->
                OnboardingAppRow(
                    app = app,
                    selected = app.packageName in selectedPackages,
                    onClick = { onToggleApp(app.packageName) },
                )
                if (index != apps.lastIndex) {
                    HorizontalDivider(color = AppBorder)
                }
            }
        }
    }
}

@Composable
private fun OnboardingAppRow(app: InstalledAppInfo, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) AppSurfaceMuted else AppSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstalledAppIcon(app)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${app.category.label} · ${app.packageName}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "선택됨", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun OnboardingFloatingBottomAction(
    step: Int,
    uiState: AILockUiState,
    onNext: () -> Unit,
    onOpenNextPermission: () -> Unit,
    onSaveAppsAndContinue: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileReady = uiState.profileDraft.name.isNotBlank() &&
        uiState.profileDraft.age != null &&
        uiState.profileDraft.gender.isNotBlank() &&
        uiState.profileDraft.job.isNotBlank()
    val allPermissionsGranted = uiState.permissions.allRequiredGranted
    val hasSelectedApps = uiState.onboardingSelectedPackages.isNotEmpty()
    val actionText = when (step) {
        0 -> "시작하기"
        1 -> if (allPermissionsGranted) "다음" else "권한 설정하기"
        2 -> "다음"
        3 -> if (hasSelectedApps) "다음" else "앱을 선택해 주세요"
        else -> "시작하기"
    }
    val actionEnabled = when (step) {
        2 -> profileReady
        3 -> hasSelectedApps
        else -> true
    }
    val actionClick = when (step) {
        1 -> if (allPermissionsGranted) onNext else onOpenNextPermission
        3 -> onSaveAppsAndContinue
        LastOnboardingStep -> onFinish
        else -> onNext
    }

    FloatingBottomActionButton(
        text = actionText,
        onClick = actionClick,
        enabled = actionEnabled,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingBottomAction(
    step: Int,
    uiState: AILockUiState,
    onNext: () -> Unit,
    onOpenNextPermission: () -> Unit,
    onSaveAppsAndContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    val profileReady = uiState.profileDraft.name.isNotBlank() &&
        uiState.profileDraft.age != null &&
        uiState.profileDraft.gender.isNotBlank() &&
        uiState.profileDraft.job.isNotBlank()
    val allPermissionsGranted = uiState.permissions.allRequiredGranted
    val hasSelectedApps = uiState.onboardingSelectedPackages.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (step) {
            0 -> PrimaryButton("시작하기", onNext, modifier = Modifier.fillMaxWidth())
            1 -> {
                PrimaryButton(
                    text = if (allPermissionsGranted) "다음" else "권한 설정하기",
                    onClick = if (allPermissionsGranted) onNext else onOpenNextPermission,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            2 -> PrimaryButton("다음", onNext, enabled = profileReady, modifier = Modifier.fillMaxWidth())
            3 -> PrimaryButton(
                text = if (hasSelectedApps) "다음" else "앱을 선택해 주세요",
                onClick = onSaveAppsAndContinue,
                enabled = hasSelectedApps,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> PrimaryButton("시작하기", onFinish, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(totalSteps) { index ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (index < currentStep) MaterialTheme.colorScheme.outlineVariant else AppSurfaceMuted,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).padding(vertical = 2.dp),
            ) {
                Box(modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun MissingPermissionText(uiState: AILockUiState) {
    val missing = buildList {
        if (!uiState.permissions.hasUsageAccess) add("사용 기록 접근")
        if (!uiState.permissions.isAccessibilityEnabled) add("접근성 서비스")
        if (!uiState.permissions.canDrawOverlays) add("다른 앱 위에 표시")
        if (!uiState.permissions.isIgnoringBatteryOptimizations) add("배터리 최적화 제외")
    }
    Text(
        text = if (missing.isEmpty()) {
            "필수 권한이 모두 허용됐어."
        } else {
            "아직 필요한 권한: ${missing.joinToString(", ")}"
        },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun readyMessage(uiState: AILockUiState): String {
    val selected = uiState.lockedApps.joinToString(", ") { it.appName }
    return if (selected.isBlank()) {
        "좋아. 이제 바로 시작해볼게."
    } else {
        "${selected}부터 같이 지켜볼게. 이제 시작해볼까?"
    }
}
