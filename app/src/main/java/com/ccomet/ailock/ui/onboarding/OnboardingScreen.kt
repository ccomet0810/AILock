package com.ccomet.ailock.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.ccomet.ailock.ui.components.RedPandaMascot
import com.ccomet.ailock.ui.components.SpeechBubbleCard
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaOrange

const val LastOnboardingStep = 11
private const val WelcomeStep = 0

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
                        text = "안녕! 앱을 조금 더 편하게 조절할 수 있도록 도와줄 AILock이야!",
                        emotion = PandaEmotion.HAPPY,
                    )
                    2 -> ExplanationStep(
                        text = "AILock은 앱을 바로 막지 않아.\n잠깐 생각할 시간을 만들어줘.",
                        cards = listOf(
                            OnboardingInfoCard("1", "앱을 열어요", "줄이고 싶은 앱을 실행하면"),
                            OnboardingInfoCard("2", "잠깐 멈춰요", "바로 막기보다 생각할 시간을 만들고"),
                            OnboardingInfoCard("3", "다시 선택해요", "계속 사용할지 함께 판단해요"),
                        ),
                    )
                    3 -> ExplanationStep(
                        text = "앱마다 하루 사용 기준 시간이 있어. 기본값은 2시간이야.\n이건 \"2시간 동안 잠금\"이라는 뜻이 아니라, 오늘 얼마나 사용했는지 판단할 때 참고하는 기준이야.",
                        cards = listOf(
                            OnboardingInfoCard("기준", "하루 2시간", "오늘 사용량을 비교하는 기준"),
                            OnboardingInfoCard("아님", "2시간 잠금", "온보딩 직후 앱을 막는 시간은 아니야"),
                        ),
                    )
                    4 -> ExplanationStep(
                        text = "선택한 앱을 열면 왜 지금 사용하려는지 물어볼게.\n사용 이유와 오늘 사용량을 보고 계속 사용할지 함께 판단해.",
                        cards = listOf(
                            OnboardingInfoCard("질문", "사용 이유 입력", "왜 지금 필요한지 짧게 적고"),
                            OnboardingInfoCard("판단", "AI가 함께 확인", "기준 시간과 이유를 함께 살펴봐요"),
                            OnboardingInfoCard("도움", "잠깐 멈춤", "무작정 막기보다 선택을 도와줘요"),
                        ),
                    )
                    5 -> CenterPandaStep(
                        text = "이제 기본 정보를 알려줄 수 있을까?\n너에게 맞는 설정을 만들어두고 싶어.",
                        emotion = PandaEmotion.THINKING,
                    )
                    6 -> ProfileStep(
                        profile = uiState.profileDraft,
                        onProfileChange = onProfileChange,
                    )
                    7 -> CenterPandaStep(
                        text = "이제 필요한 권한을 허용해줄 수 있을까?\n그래야 AILock이 제대로 도와줄 수 있어.",
                        emotion = PandaEmotion.ENCOURAGING,
                    )
                    8 -> PermissionsStep(
                        uiState = uiState,
                        onUsagePermission = onUsagePermission,
                        onOverlayPermission = onOverlayPermission,
                        onAccessibilityPermission = onAccessibilityPermission,
                        onNotificationPermission = onNotificationPermission,
                        onBatteryPermission = onBatteryPermission,
                    )
                    9 -> CenterPandaStep(
                        text = "이제 관리할 앱을 골라볼까?\n어떤 앱을 도와주면 좋을지 알려줘.",
                        emotion = PandaEmotion.DEFAULT,
                    )
                    10 -> AppsStep(
                        uiState = uiState,
                        onQuery = onAppQuery,
                        onToggleApp = onToggleApp,
                    )
                    11 -> CenterPandaStep(
                        text = readyMessage(uiState),
                        emotion = PandaEmotion.ENCOURAGING,
                    )
                }
            }
            OnboardingFloatingBottomAction(
                step = step,
                uiState = uiState,
                onNext = onNext,
                onOpenNextPermission = onOpenNextPermission,
                onSaveProfileAndContinue = onSaveProfileAndContinue,
                onSaveAppsAndContinue = onSaveAppsAndContinue,
                onFinish = onFinish,
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
private fun HeaderPandaMessage(text: String, emotion: PandaEmotion = PandaEmotion.DEFAULT) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpeechBubbleCard(text = text, modifier = Modifier.fillMaxWidth())
        RedPandaMascot(emotion = emotion, modifier = Modifier.size(64.dp))
    }
}

private data class OnboardingInfoCard(
    val badge: String,
    val title: String,
    val description: String,
)

@Composable
private fun ExplanationStep(
    text: String,
    cards: List<OnboardingInfoCard>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap)) {
        HeaderPandaMessage(text = text, emotion = PandaEmotion.THINKING)
        Column(verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap)) {
            cards.forEach { card ->
                AilockCard(contentPadding = PaddingValues(AILockSpacing.itemPadding)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PandaOrange.copy(alpha = 0.14f), AILockShape.control),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = card.badge,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PandaOrange,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(card.title, fontWeight = FontWeight.Bold, color = AppTextStrong)
                            Text(
                                card.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTextSubtle,
                            )
                        }
                    }
                }
            }
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap), modifier = Modifier.fillMaxWidth()) {
        HeaderPandaMessage(
            text = "필요한 권한을 허용해줘.\n권한이 있어야 AILock을 사용할 수 있어.",
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
    Column(verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap), modifier = Modifier.fillMaxWidth()) {
        HeaderPandaMessage(
            text = "기본 정보를 입력해줘.\n너에게 맞는 설정을 만드는 데 사용할게.",
            emotion = PandaEmotion.THINKING,
        )
        AilockOutlinedTextField(
            value = profile.name,
            onValueChange = { onProfileChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = "이름",
        )
        AilockOutlinedTextField(
            value = profile.age?.toString().orEmpty(),
            onValueChange = { onProfileChange(profile.copy(age = it.toIntOrNull())) },
            modifier = Modifier.fillMaxWidth(),
            label = "나이",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap), modifier = Modifier.fillMaxWidth()) {
            AilockOutlinedTextField(
                value = profile.gender,
                onValueChange = { onProfileChange(profile.copy(gender = it)) },
                modifier = Modifier.weight(1f),
                label = "성별",
            )
            AilockOutlinedTextField(
                value = profile.job,
                onValueChange = { onProfileChange(profile.copy(job = it)) },
                modifier = Modifier.weight(1f),
                label = "직업",
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

    Column(verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap), modifier = Modifier.fillMaxWidth()) {
        HeaderPandaMessage(
            text = if (selectedApps.isEmpty()) {
                "관리할 앱을 선택해줘.\n나중에 언제든 추가하거나 삭제할 수 있어."
            } else {
                "좋아. ${selectedNames}은 하루 사용 기준 2시간으로 저장할게.\n바로 잠그는 시간이 아니라 사용량 판단 기준이야."
            },
            emotion = PandaEmotion.DEFAULT,
        )
        AilockOutlinedTextField(
            value = uiState.appQuery,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            label = "앱 이름 검색",
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
        shape = AILockShape.card,
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
            .padding(AILockSpacing.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
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
    onSaveProfileAndContinue: () -> Unit,
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
        1 -> "안녕!"
        2, 3 -> "다음"
        4 -> "이해했어!"
        5 -> "기본 정보 입력하기"
        6 -> "다음"
        7 -> "권한 설정하기"
        8 -> if (allPermissionsGranted) "다음" else "권한 설정하기"
        9 -> "앱 고르기"
        10 -> if (hasSelectedApps) "선택 완료" else "앱을 선택해줘"
        else -> "시작하기"
    }
    val actionEnabled = when (step) {
        6 -> profileReady
        10 -> hasSelectedApps
        else -> true
    }
    val actionClick = when (step) {
        6 -> onSaveProfileAndContinue
        8 -> if (allPermissionsGranted) onNext else onOpenNextPermission
        10 -> onSaveAppsAndContinue
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
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
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

private fun readyMessage(uiState: AILockUiState): String {
    val selected = uiState.lockedApps.joinToString(", ") { it.appName }
    return if (selected.isBlank()) {
        "좋아! 이제 AILock이 준비됐어.\n잠깐 멈추고 생각할 수 있도록 도와줄게!\n같이 시작해보자!"
    } else {
        "좋아! 이제 ${selected} 준비가 끝났어.\nAILock이 잠깐 멈추고 생각할 수 있도록 도와줄게!\n같이 시작해보자!"
    }
}
