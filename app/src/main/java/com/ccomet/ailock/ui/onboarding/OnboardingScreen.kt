package com.ccomet.ailock.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PermissionCards
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.RedPandaMascot
import com.ccomet.ailock.ui.components.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: AILockUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onUsagePermission: () -> Unit,
    onOverlayPermission: () -> Unit,
    onAccessibilityPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onDebugContinue: () -> Unit,
    onProfileChange: (UserProfile) -> Unit,
    onSaveProfileAndContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AILock 시작하기") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = uiState.onboardingStep > 0) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                progress = { (uiState.onboardingStep + 1) / 5f },
                modifier = Modifier.fillMaxWidth(),
            )
            when (uiState.onboardingStep) {
                0 -> WelcomeStep(onNext)
                1 -> HelpStep(onNext)
                2 -> PermissionStep(
                    uiState = uiState,
                    onUsagePermission = onUsagePermission,
                    onOverlayPermission = onOverlayPermission,
                    onAccessibilityPermission = onAccessibilityPermission,
                    onNotificationPermission = onNotificationPermission,
                    onRefreshPermissions = onRefreshPermissions,
                    onNext = onNext,
                    onDebugContinue = onDebugContinue,
                )

                3 -> ProfileStep(
                    profile = uiState.profileDraft,
                    onProfileChange = onProfileChange,
                    onSaveProfileAndContinue = onSaveProfileAndContinue,
                )

                else -> DoneStep(onFinish)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Spacer(Modifier.padding(8.dp))
    RedPandaMascot(PandaEmotion.HAPPY)
    Text("안녕!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(
        "나는 네 스마트폰 약속을 옆에서 같이 지켜볼 레서판다야.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryButton("시작하기", onClick = onNext, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun HelpStep(onNext: () -> Unit) {
    PandaSpeechBubble(
        text = "스마트폰 줄이는 거, 혼자 하면 어렵지? 내가 옆에서 같이 도와줄게!",
        emotion = PandaEmotion.ENCOURAGING,
    )
    PrimaryButton("다음", onClick = onNext, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PermissionStep(
    uiState: AILockUiState,
    onUsagePermission: () -> Unit,
    onOverlayPermission: () -> Unit,
    onAccessibilityPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onNext: () -> Unit,
    onDebugContinue: () -> Unit,
) {
    PandaSpeechBubble(
        text = "내가 옆에서 도와주려면 몇 가지 권한이 필요해!",
        emotion = PandaEmotion.THINKING,
    )
    PermissionCards(
        permissionState = uiState.permissions,
        onUsage = onUsagePermission,
        onOverlay = onOverlayPermission,
        onAccessibility = onAccessibilityPermission,
        onNotification = onNotificationPermission,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton("상태 새로고침", onClick = onRefreshPermissions, modifier = Modifier.weight(1f))
        PrimaryButton(
            "다음",
            onClick = onNext,
            modifier = Modifier.weight(1f),
            enabled = uiState.permissions.allRequiredGranted,
            icon = Icons.Default.Check,
        )
    }
    SecondaryButton(
        "권한 없이 데모 계속하기",
        onClick = onDebugContinue,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileStep(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onSaveProfileAndContinue: () -> Unit,
) {
    PandaSpeechBubble(
        text = "너에 대해 조금만 알려줘! 이 정보는 앱 안에서 맞춤 문구와 기록에만 사용할게.",
        emotion = PandaEmotion.DEFAULT,
    )
    OutlinedTextField(
        value = profile.name,
        onValueChange = { onProfileChange(profile.copy(name = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("이름") },
        singleLine = true,
    )
    OutlinedTextField(
        value = profile.age?.toString().orEmpty(),
        onValueChange = { raw -> onProfileChange(profile.copy(age = raw.toIntOrNull())) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("나이") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    OutlinedTextField(
        value = profile.gender,
        onValueChange = { onProfileChange(profile.copy(gender = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("성별") },
        singleLine = true,
    )
    OutlinedTextField(
        value = profile.job,
        onValueChange = { onProfileChange(profile.copy(job = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("직업") },
        singleLine = true,
    )
    PrimaryButton(
        "다음",
        onClick = onSaveProfileAndContinue,
        modifier = Modifier.fillMaxWidth(),
        enabled = profile.name.isNotBlank() && profile.gender.isNotBlank() && profile.job.isNotBlank(),
    )
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    RedPandaMascot(PandaEmotion.HAPPY)
    PandaSpeechBubble(
        text = "좋아! 이제 내가 네 스마트폰 사용을 같이 지켜봐줄게.",
        emotion = PandaEmotion.HAPPY,
    )
    PrimaryButton("튜토리얼 보기", onClick = onFinish, modifier = Modifier.fillMaxWidth())
    SecondaryButton("괜찮아요, 바로 시작할게요", onClick = onFinish, modifier = Modifier.fillMaxWidth())
}
