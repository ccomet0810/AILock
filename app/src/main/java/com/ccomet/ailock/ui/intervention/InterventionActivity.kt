package com.ccomet.ailock.ui.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.data.model.AiDecisionRequest
import com.ccomet.ailock.data.model.AiDecisionResponse
import com.ccomet.ailock.data.model.AiDecisionStatus
import com.ccomet.ailock.data.model.CurrentStats
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.data.model.UsageEventType
import com.ccomet.ailock.service.AILockAccessibilityService
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.theme.AILockTheme
import com.ccomet.ailock.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val timeLimitExceeded = intent.getBooleanExtra(EXTRA_TIME_LIMIT_EXCEEDED, false)
        val pledgeMode = intent.getBooleanExtra(EXTRA_PLEDGE_MODE, false)
        setContent {
            AILockTheme {
                InterventionRoute(
                    packageName = packageName,
                    timeLimitExceeded = timeLimitExceeded,
                    pledgeMode = pledgeMode,
                    onClose = { finish() },
                    onHome = {
                        goHome()
                        finish()
                    },
                )
            }
        }
    }

    private fun goHome() {
        if (AILockAccessibilityService.goHomeIfAvailable()) return
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    companion object {
        private const val EXTRA_PACKAGE_NAME = "packageName"
        private const val EXTRA_TIME_LIMIT_EXCEEDED = "timeLimitExceeded"
        private const val EXTRA_PLEDGE_MODE = "pledgeMode"

        fun intent(
            context: Context,
            packageName: String,
            timeLimitExceeded: Boolean,
            pledgeMode: Boolean = false,
        ): Intent = Intent(context, InterventionActivity::class.java)
            .putExtra(EXTRA_PACKAGE_NAME, packageName)
            .putExtra(EXTRA_TIME_LIMIT_EXCEEDED, timeLimitExceeded)
            .putExtra(EXTRA_PLEDGE_MODE, pledgeMode)
    }
}

@Composable
private fun InterventionRoute(
    packageName: String,
    timeLimitExceeded: Boolean,
    pledgeMode: Boolean,
    onClose: () -> Unit,
    onHome: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember { AILockContainer.get(context) }
    val lockedApps by container.ailockRepository.lockedApps.collectAsState(initial = emptyList())
    val config = lockedApps.firstOrNull { it.packageName == packageName }

    if (config == null) {
        MissingConfigScreen(onClose)
        return
    }

    if (pledgeMode) {
        PledgeScreen(config = config, onClose = onClose, onHome = onHome)
    } else {
        ReasonScreen(config = config, timeLimitExceeded = timeLimitExceeded, onClose = onClose, onHome = onHome)
    }
}

@Composable
private fun ReasonScreen(
    config: LockedAppConfig,
    timeLimitExceeded: Boolean,
    onClose: () -> Unit,
    onHome: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember { AILockContainer.get(context) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var response by remember { mutableStateOf<AiDecisionResponse?>(null) }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PandaSpeechBubble(
                text = if (timeLimitExceeded) {
                    "너 1분 지났는데 뭐 하는 거야? 약속 시간 끝났어. 계속 써야 하는 이유가 있어?"
                } else {
                    "잠깐! ${config.appName} 왜 켰어? 나한테 이유를 말해줘."
                },
                emotion = if (timeLimitExceeded) PandaEmotion.ANGRY else PandaEmotion.SUSPICIOUS,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("사용 이유") },
                placeholder = { Text("예: 과제 자료를 확인해야 해.") },
            )
            if (loading) {
                CircularProgressIndicator()
                Text("흠... 잠깐만. 이 이유가 괜찮은지 생각해볼게.")
            }
            response?.let { decision ->
                PandaSpeechBubble(
                    text = "${decision.text}\n${sourceLabel(decision.source)} · 허용 시간: ${decision.allowedTime}분",
                    emotion = emotionFor(decision.status),
                )
            }
            PrimaryButton(
                text = when {
                    response == null -> "레서판다에게 물어보기"
                    response?.source == "network-error" -> "설정 확인할게"
                    (response?.allowedTime ?: 0) > 0 -> "${config.appName} 사용하기"
                    else -> "그래도 사용할래"
                },
                onClick = {
                    val current = response
                    if (current == null) {
                        scope.launch {
                            loading = true
                            response = requestDecision(container, config, input)
                            loading = false
                        }
                    } else if (current.source == "network-error") {
                        onClose()
                    } else if (current.allowedTime > 0 && current.status != AiDecisionStatus.CRITICAL) {
                        container.ailockRepository.grantTemporaryAllowance(config.packageName, current.allowedTime)
                        scope.launch {
                            container.ailockRepository.recordEvent(config.packageName, config.appName, UsageEventType.AI_ALLOWED, aiStatus = current.status.name, aiAllowedTime = current.allowedTime)
                        }
                        onClose()
                    } else {
                        scope.launch {
                            container.ailockRepository.recordEvent(config.packageName, config.appName, UsageEventType.AI_DENIED, aiStatus = current.status.name, aiAllowedTime = current.allowedTime)
                            container.ailockRepository.adjustWillPower(-5)
                        }
                        context.startActivity(InterventionActivity.intent(context, config.packageName, timeLimitExceeded = true, pledgeMode = true))
                        onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && (response != null || input.isNotBlank()),
            )
            SecondaryButton(
                text = "이번엔 참아볼게",
                onClick = {
                    scope.launch {
                        container.ailockRepository.recordEvent(config.packageName, config.appName, UsageEventType.SELF_STOP)
                        container.ailockRepository.adjustWillPower(+3)
                    }
                    onHome()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PledgeScreen(
    config: LockedAppConfig,
    onClose: () -> Unit,
    onHome: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember { AILockContainer.get(context) }
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf(false) }
    var second by remember { mutableStateOf(false) }
    var third by remember { mutableStateOf(false) }
    val enabled = first && second && third

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PandaSpeechBubble(
                text = "정말 약속을 어길 거야?",
                emotion = PandaEmotion.DISAPPOINTED,
            )
            Text("서약서", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            CheckLine("저는 레서판다와의 약속을 어기겠습니다.", first) { first = it }
            CheckLine("저는 지금 충동적으로 앱을 사용하려고 합니다.", second) { second = it }
            CheckLine("이 선택으로 오늘의 기록이 나빠질 수 있음을 알고 있습니다.", third) { third = it }
            SecondaryButton("아니, 이제 멈출래", onClick = {
                scope.launch {
                    container.ailockRepository.recordEvent(config.packageName, config.appName, UsageEventType.SELF_STOP)
                    container.ailockRepository.adjustWillPower(+3)
                }
                onHome()
            }, modifier = Modifier.fillMaxWidth())
            PrimaryButton("그래도 사용하러 갈래", onClick = {
                scope.launch {
                    container.ailockRepository.recordEvent(config.packageName, config.appName, UsageEventType.PLEDGE)
                    container.ailockRepository.adjustWillPower(-10)
                }
                onClose()
            }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
        }
    }
}

@Composable
private fun CheckLine(text: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MissingConfigScreen(onClose: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            PandaSpeechBubble(
                text = "이 앱의 제한 설정을 찾지 못했어. AILock 설정을 다시 확인해줘.",
                emotion = PandaEmotion.THINKING,
            )
            PrimaryButton("닫기", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

private suspend fun requestDecision(
    container: AILockContainer,
    config: LockedAppConfig,
    input: String,
): AiDecisionResponse {
    val records = container.ailockRepository.usageRecords.first()
    val today = TimeUtils.todayRecords(records).filter { it.packageName == config.packageName }
    val request = AiDecisionRequest(
        appName = config.appName,
        userInput = input,
        lockReason = config.lockReasonFinal,
        currentStats = CurrentStats(
            willPowerScore = container.ailockRepository.willPowerScore.first(),
            todayOpenAppCount = today.count { it.eventType == UsageEventType.OPEN || it.eventType == UsageEventType.AI_REQUEST },
            accumUseApp = today.sumOf { it.durationMinutes },
        ),
    )
    container.ailockRepository.recordEvent(
        packageName = config.packageName,
        appName = config.appName,
        eventType = UsageEventType.AI_REQUEST,
        userInput = input,
        lockReason = config.lockReasonFinal,
    )
    container.ailockRepository.adjustWillPower(-2)
    return container.aiDecisionRepository.decide(
        request = request,
        baseUrl = container.ailockRepository.backendBaseUrl.first(),
        useMock = container.ailockRepository.mockAiMode.first(),
    )
}

private fun emotionFor(status: AiDecisionStatus): PandaEmotion = when (status) {
    AiDecisionStatus.OPTIMAL -> PandaEmotion.HAPPY
    AiDecisionStatus.WARNING -> PandaEmotion.ENCOURAGING
    AiDecisionStatus.OVERUSE -> PandaEmotion.ANGRY
    AiDecisionStatus.CRITICAL -> PandaEmotion.SAD
    AiDecisionStatus.FAIL -> PandaEmotion.THINKING
}

private fun sourceLabel(source: String?): String = when (source) {
    "server" -> "실제 서버 응답"
    "mock" -> "Mock 판단"
    "network-error" -> "서버 연결 실패"
    else -> "판단 결과"
}
