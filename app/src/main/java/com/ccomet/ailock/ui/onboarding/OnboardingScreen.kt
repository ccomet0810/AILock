package com.ccomet.ailock.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.AilockOutlinedTextField
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PermissionCards
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.RedPandaMascot
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.components.SpeechBubbleCard
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle

private const val WelcomeStep = 0
private const val LastOnboardingStep = 6
private const val NameInputStep = 3
private const val PermissionStep = 4
private const val AppSelectionStep = 5

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
    onSaveProfileAndContinue: () -> Unit,
    onAppQuery: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onDailyLimit: (String, Int) -> Unit,
    onSaveAppsAndContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    val step = uiState.onboardingStep.coerceIn(WelcomeStep, LastOnboardingStep)

    if (step == WelcomeStep) {
        WelcomeScreen(onStart = onNext)
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AILock", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (step > 1) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전")
                        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AILockSpacing.screenHorizontal, vertical = AILockSpacing.contentVertical)
                    .padding(bottom = AILockLayout.scrollContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap),
            ) {
                StepIndicator(currentStep = step, totalSteps = LastOnboardingStep)
                when (step) {
                    1 -> CenterPandaStep(
                        text = "안녕!\n앱 사용을 조금 더 편하게 조절할 수 있도록 도와줄게.",
                        emotion = PandaEmotion.HAPPY,
                    )
                    2 -> CenterPandaStep(
                        text = "앱마다 하루 사용 기준 시간이 있어.\n기본값은 2시간이야.\n추후에 언제든 수정할 수 있어.",
                        emotion = PandaEmotion.THINKING,
                    )
                    3 -> NameInputStep(
                        profile = uiState.profileDraft,
                        onProfileChange = onProfileChange,
                    )
                    4 -> PermissionsStep(
                        uiState = uiState,
                        onUsagePermission = onUsagePermission,
                        onOverlayPermission = onOverlayPermission,
                        onAccessibilityPermission = onAccessibilityPermission,
                        onNotificationPermission = onNotificationPermission,
                        onBatteryPermission = onBatteryPermission,
                    )
                    5 -> AppSelectionStep(
                        uiState = uiState,
                        onQuery = onAppQuery,
                        onToggleApp = onToggleApp,
                        onDailyLimit = onDailyLimit,
                    )
                    6 -> CenterPandaStep(
                        text = "좋아!\n그럼 이제 시작해볼까?",
                        emotion = PandaEmotion.ENCOURAGING,
                    )
                }
            }
            FloatingBottomActionButton(
                text = when (step) {
                    AppSelectionStep -> "선택 완료"
                    1 -> "안녕!"
                    2, 3 -> "다음"
                    4 -> if (uiState.permissions.allRequiredGranted) "다음" else "권한 설정하기"
                    else -> "시작하기"
                },
                onClick = when (step) {
                    AppSelectionStep -> onSaveAppsAndContinue
                    NameInputStep -> onSaveProfileAndContinue
                    PermissionStep -> if (uiState.permissions.allRequiredGranted) onNext else onOpenNextPermission
                    LastOnboardingStep -> onFinish
                    else -> onNext
                },
                enabled = when (step) {
                    NameInputStep -> uiState.profileDraft.name.isNotBlank()
                    AppSelectionStep -> uiState.onboardingSelectedApps.isNotEmpty()
                    else -> true
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun WelcomeScreen(onStart: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AILockSpacing.screenHorizontal, vertical = AILockSpacing.sectionGap),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = AILockLayout.bottomActionAreaHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "AILock",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTextStrong,
                )
                Spacer(Modifier.height(AILockSpacing.sectionGap))
                SpeechBubbleCard(
                    text = "AILock 파일럿 테스트에 참여해주셔서 진심으로 감사드립니다.",
                    modifier = Modifier.fillMaxWidth(),
                )
                RedPandaMascot(
                    emotion = PandaEmotion.HAPPY,
                    modifier = Modifier
                        .padding(top = AILockSpacing.listGap)
                        .size(132.dp),
                )
            }
            FloatingBottomActionButton(
                text = "시작하기",
                onClick = onStart,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CenterPandaStep(text: String, emotion: PandaEmotion) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp),
        contentAlignment = Alignment.Center,
    ) {
        PandaSpeechBubble(text = text, emotion = emotion)
    }
}

@Composable
private fun NameInputStep(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PandaSpeechBubble(
            text = "이름만 알려주면 바로 시작할 수 있어.",
            emotion = PandaEmotion.THINKING,
        )
        Spacer(Modifier.height(AILockSpacing.sectionGap))
        AilockOutlinedTextField(
            value = profile.name,
            onValueChange = { onProfileChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = "이름",
        )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap),
    ) {
        PandaSpeechBubble(
            text = "AILock이 제대로 도와주려면\n몇 가지 권한이 필요해.",
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
private fun AppSelectionStep(
    uiState: AILockUiState,
    onQuery: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onDailyLimit: (String, Int) -> Unit,
) {
    val selectedPackages = uiState.onboardingSelectedPackages
    val selectableApps = uiState.installedApps.filter { !it.isLocked || it.packageName in selectedPackages }
    var timerApp by remember { mutableStateOf<InstalledAppInfo?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap),
    ) {
        PandaSpeechBubble(
            text = "관리할 앱을 골라줘.\n선택한 앱에는 아래 기준 시간이 적용돼.",
            emotion = PandaEmotion.THINKING,
        )
        AilockOutlinedTextField(
            value = uiState.appQuery,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            label = "앱 이름 검색",
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        AilockCard(contentPadding = PaddingValues(0.dp)) {
            Column {
                if (selectableApps.isEmpty()) {
                    Text(
                        text = "선택할 수 있는 앱이 없어요.",
                        modifier = Modifier.padding(AILockSpacing.itemPadding),
                        color = AppTextSubtle,
                    )
                } else {
                    selectableApps.take(24).forEachIndexed { index, app ->
                        OnboardingAppRow(
                            app = app,
                            selected = app.packageName in selectedPackages,
                            timerMinutes = uiState.onboardingAppDailyLimits[app.packageName],
                            onClick = {
                                val wasSelected = app.packageName in selectedPackages
                                onToggleApp(app.packageName)
                                if (!wasSelected) {
                                    timerApp = app
                                }
                            },
                        )
                        if (index != selectableApps.take(24).lastIndex) {
                            HorizontalDivider(color = AppBorder)
                        }
                    }
                }
            }
        }
    }

    timerApp?.let { app ->
        AppTimerDialog(
            appName = app.appName,
            minutes = uiState.onboardingAppDailyLimits[app.packageName] ?: 120,
            onDailyLimit = { minutes -> onDailyLimit(app.packageName, minutes) },
            onDismiss = { timerApp = null },
        )
    }
}

@Composable
private fun OnboardingAppRow(
    app: InstalledAppInfo,
    selected: Boolean,
    timerMinutes: Int?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) AppSurfaceMuted else AppSurface)
            .padding(AILockSpacing.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
    ) {
        InstalledAppIcon(app)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (selected) "${formatTimerLabel(timerMinutes ?: 120)} 기준" else app.packageName,
                color = AppTextSubtle,
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
private fun AppTimerDialog(
    appName: String,
    minutes: Int,
    onDailyLimit: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AilockCard(
            modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            Text(appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppTextStrong)
            DialogTimerContent(
                minutes = minutes,
                onDailyLimit = onDailyLimit,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap)) {
                SecondaryButton(
                    text = "취소",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "설정 완료",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogTimerContent(
    minutes: Int,
    onDailyLimit: (Int) -> Unit,
) {
    val hours = minutes / 60
    val mins = minutes % 60

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap),
    ) {
        Text("하루 사용 기준 시간", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppTextStrong)
        Text("이 시간은 저장 후 수정할 수 없어요.", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerValueColumn(label = "시간", value = hours.toString().padStart(2, '0'))
            Text(
                text = " : ",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = AppTextSubtle,
            )
            TimerValueColumn(label = "분", value = mins.toString().padStart(2, '0'))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap)) {
            TimerStepGroup(
                label = "시간",
                onDecrease = { onDailyLimit(clampOnboardingTimer(minutes - 60)) },
                onIncrease = { onDailyLimit(clampOnboardingTimer(minutes + 60)) },
            )
            TimerStepGroup(
                label = "분",
                onDecrease = { onDailyLimit(clampOnboardingTimer(minutes - 10)) },
                onIncrease = { onDailyLimit(clampOnboardingTimer(minutes + 10)) },
            )
        }
    }
}

@Composable
private fun OnboardingTimerSettingSection(
    minutes: Int,
    onDailyLimit: (Int) -> Unit,
) {
    val hours = minutes / 60
    val mins = minutes % 60

    AilockCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap),
        ) {
            Text("하루 사용 기준 시간", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("나중에 제한 탭에서 확인할 수 있어요.", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimerValueColumn(label = "시", value = hours.toString().padStart(2, '0'))
                Text(
                    text = " : ",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTextSubtle,
                )
                TimerValueColumn(label = "분", value = mins.toString().padStart(2, '0'))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap)) {
                TimerStepGroup(
                    label = "시",
                    onDecrease = { onDailyLimit(clampOnboardingTimer(minutes - 60)) },
                    onIncrease = { onDailyLimit(clampOnboardingTimer(minutes + 60)) },
                )
                TimerStepGroup(
                    label = "분",
                    onDecrease = { onDailyLimit(clampOnboardingTimer(minutes - 10)) },
                    onIncrease = { onDailyLimit(clampOnboardingTimer(minutes + 10)) },
                )
            }
        }
    }
}

@Composable
private fun TimerValueColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppTextSubtle)
        Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = AppTextStrong)
    }
}

@Composable
private fun TimerStepGroup(
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppTextSubtle)
        Row {
            TimerStepButton(label = "$label 줄이기", icon = Icons.Default.Remove, onClick = onDecrease)
            TimerStepButton(label = "$label 늘리기", icon = Icons.Default.Add, onClick = onIncrease)
        }
    }
}

@Composable
private fun TimerStepButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label)
    }
}

private fun clampOnboardingTimer(minutes: Int): Int = minutes.coerceIn(0, 23 * 60 + 59)

private fun formatTimerLabel(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}시간 ${mins}분"
        hours > 0 -> "${hours}시간"
        else -> "${mins}분"
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        repeat(totalSteps) { index ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (index < currentStep) MaterialTheme.colorScheme.outlineVariant else AppSurfaceMuted,
                ),
                shape = AILockShape.pill,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
            ) {
                Box(modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
