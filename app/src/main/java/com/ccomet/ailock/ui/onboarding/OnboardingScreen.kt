package com.ccomet.ailock.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AilockOutlinedTextField
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PermissionCards
import com.ccomet.ailock.ui.components.RedPandaMascot
import com.ccomet.ailock.ui.components.SpeechBubbleCard
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong

private const val WelcomeStep = 0
private const val LastOnboardingStep = 5
private const val NameInputStep = 3
private const val PermissionStep = 4

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
                    5 -> CenterPandaStep(
                        text = "좋아!\n그럼 이제 시작해볼까?",
                        emotion = PandaEmotion.ENCOURAGING,
                    )
                }
            }
            FloatingBottomActionButton(
                text = when (step) {
                    1 -> "안녕!"
                    2, 3 -> "다음"
                    4 -> if (uiState.permissions.allRequiredGranted) "다음" else "권한 설정하기"
                    else -> "시작하기"
                },
                onClick = when (step) {
                    NameInputStep -> onSaveProfileAndContinue
                    PermissionStep -> if (uiState.permissions.allRequiredGranted) onNext else onOpenNextPermission
                    LastOnboardingStep -> onFinish
                    else -> onNext
                },
                enabled = step != NameInputStep || uiState.profileDraft.name.isNotBlank(),
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
