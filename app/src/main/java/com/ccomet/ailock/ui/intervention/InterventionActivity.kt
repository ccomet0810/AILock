package com.ccomet.ailock.ui.intervention

import android.content.Context
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.ActiveUseSession
import com.ccomet.ailock.data.model.JudgePostRequest
import com.ccomet.ailock.data.model.JudgePreRequest
import com.ccomet.ailock.data.model.PendingFinalDecision
import com.ccomet.ailock.service.AILockAccessibilityService
import com.ccomet.ailock.service.OverlayService
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.components.SpeechBubbleCard
import com.ccomet.ailock.ui.theme.AILockTheme
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppBorderStrong
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private const val SESSION_STATE_IN_USE = "IN_USE"

class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawable(TRANSPARENT.toDrawable())
        window.setDimAmount(0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(36)
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val timeLimitExceeded = intent.getBooleanExtra(EXTRA_TIME_LIMIT_EXCEEDED, false)

        setContent {
            AILockTheme {
                InterventionRoute(
                    packageName = packageName,
                    timeLimitExceeded = timeLimitExceeded,
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

        fun intent(context: Context, packageName: String, timeLimitExceeded: Boolean): Intent =
            Intent(context, InterventionActivity::class.java)
                .putExtra(EXTRA_PACKAGE_NAME, packageName)
                .putExtra(EXTRA_TIME_LIMIT_EXCEEDED, timeLimitExceeded)
    }
}

private enum class InterventionScreenState {
    INPUT,
    LOADING,
    RESULT,
    ERROR,
}

@Composable
private fun InterventionRoute(
    packageName: String,
    timeLimitExceeded: Boolean,
    onClose: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val container = remember { AILockContainer.get(context) }
    val scope = rememberCoroutineScope()
    val lockedApps = container.ailockRepository.lockedApps.collectAsState(initial = emptyList()).value
    val config = lockedApps.firstOrNull { it.packageName == packageName }

    if (config == null) {
        MissingConfigScreen(onClose)
        return
    }

    var screenState by remember { mutableStateOf(InterventionScreenState.INPUT) }
    var reasonInput by remember { mutableStateOf("") }
    var activeSession by remember { mutableStateOf<ActiveUseSession?>(null) }
    var pendingDecision by remember { mutableStateOf<PendingFinalDecision?>(null) }
    var isAdditionalRequest by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(packageName, timeLimitExceeded) {
        val session = container.activeUseSessionRepository.get(packageName)
        val pending = container.pendingFinalDecisionRepository.get(packageName)
        activeSession = session
        pendingDecision = pending
        isAdditionalRequest = session != null && (timeLimitExceeded || System.currentTimeMillis() >= session.expectedEndAt)
        screenState = if (pending != null) InterventionScreenState.RESULT else InterventionScreenState.INPUT
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xCC17110E))
                .imePadding(),
        ) {
            when (screenState) {
                InterventionScreenState.INPUT -> InputScreen(
                    appName = config.appName,
                    remainingMinutes = remainingInputMinutes(config.dailyLimitMinutes, activeSession),
                    value = reasonInput,
                    isAdditionalRequest = isAdditionalRequest,
                    onValueChange = { reasonInput = it },
                    onAsk = {
                        if (reasonInput.isBlank()) return@InputScreen
                        screenState = InterventionScreenState.LOADING
                        scope.launch {
                            runCatching {
                                if (isAdditionalRequest) {
                                    val session = activeSession ?: container.activeUseSessionRepository.get(config.packageName)
                                    requireNotNull(session)
                                    val post = container.ollamaDecisionRepository.judgePost(
                                        JudgePostRequest(
                                            appName = config.appName,
                                            previousReason = session.preInput,
                                            postInput = reasonInput,
                                            requestCount = 1,
                                        ),
                                    )
                                    PendingFinalDecision(
                                        sessionId = session.sessionId.ifBlank { "ollama-session-local" },
                                        status = post.status,
                                        allowedTime = post.allowedTime,
                                        supportMessage = post.supportMessage,
                                    )
                                } else {
                                    val requestedMinutes = (config.dailyLimitMinutes ?: 10).coerceIn(1, 30)
                                    val pre = container.ollamaDecisionRepository.judgePre(
                                        JudgePreRequest(
                                            appName = config.appName,
                                            preInput = reasonInput,
                                            requestUseTime = requestedMinutes,
                                        ),
                                    )
                                    PendingFinalDecision(
                                        sessionId = pre.sessionId,
                                        status = pre.status,
                                        allowedTime = pre.allowedTime,
                                        supportMessage = pre.supportMessage,
                                    )
                                }
                            }.onSuccess { decision ->
                                pendingDecision = decision
                                screenState = InterventionScreenState.RESULT
                            }.onFailure {
                                errorMessage = "판단 중 문제가 생겼어요. 다시 시도해 주세요."
                                screenState = InterventionScreenState.ERROR
                            }
                        }
                    },
                    onGiveUp = onHome,
                    modifier = Modifier.fillMaxSize(),
                )

                InterventionScreenState.LOADING -> LoadingScreen(modifier = Modifier.fillMaxSize())

                InterventionScreenState.RESULT -> ResultScreen(
                    decision = pendingDecision,
                    onUseApp = {
                        val allowedTime = pendingDecision?.allowedTime ?: 0
                        if (allowedTime <= 0) {
                            scope.launch {
                                closeSession(container, config.packageName)
                                onHome()
                            }
                            return@ResultScreen
                        }
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val session = activeSession?.copy(
                                expectedEndAt = now + allowedTime * 60_000L,
                                state = SESSION_STATE_IN_USE,
                            ) ?: ActiveUseSession(
                                packageName = config.packageName,
                                appName = config.appName,
                                sessionId = pendingDecision?.sessionId.orEmpty(),
                                preInput = reasonInput,
                                plannedMinutes = allowedTime,
                                startedAt = now,
                                expectedEndAt = now + allowedTime * 60_000L,
                                state = SESSION_STATE_IN_USE,
                            )
                            container.activeUseSessionRepository.save(session)
                            container.pendingFinalDecisionRepository.clear(config.packageName)
                            container.ailockRepository.grantTemporaryAllowance(config.packageName, allowedTime)
                            scheduleTimeExpiredIntervention(appContext, container, session)
                            onClose()
                        }
                    },
                    onGiveUp = {
                        scope.launch {
                            closeSession(container, config.packageName)
                            onHome()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                InterventionScreenState.ERROR -> ErrorScreen(
                    message = errorMessage ?: "처리 중 문제가 발생했어요.",
                    onRetry = { screenState = InterventionScreenState.INPUT },
                    onHome = onHome,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun InputScreen(
    appName: String,
    remainingMinutes: Int,
    value: String,
    isAdditionalRequest: Boolean,
    onValueChange: (String) -> Unit,
    onAsk: () -> Unit,
    onGiveUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .padding(bottom = 248.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpeechCard(
                title = appName.uppercase(),
                subtitle = "남은 시간 ${minutesLabel(remainingMinutes)}",
            )
            Spacer(Modifier.height(12.dp))
            PandaImage(size = 132)
        }
        BottomInputPanel(
            title = if (isAdditionalRequest) "왜 더 필요해?" else "왜 켰냐?",
            value = value,
            onValueChange = onValueChange,
            onAsk = onAsk,
            onGiveUp = onGiveUp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpeechCard(title = "흠.........")
            Spacer(Modifier.height(18.dp))
            PandaImage(size = 128)
        }
        Text(
            text = "잠시만 기다려주세요\n잠시 후 결과가 도출됩니다",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            color = AppTextSubtle,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultScreen(
    decision: PendingFinalDecision?,
    onUseApp: () -> Unit,
    onGiveUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allowedTime = decision?.allowedTime ?: 0
    val message = decision?.supportMessage?.lineSequence()?.firstOrNull()?.ifBlank { null }
        ?: if (allowedTime > 0) "필요한 만큼만 확인하고 바로 돌아와." else "지금은 멈추는 게 좋아."

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PandaImage(size = 140)
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 28.dp),
                color = AppTextStrong,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatTimer(allowedTime),
                color = AppTextSubtle,
                style = MaterialTheme.typography.titleMedium,
            )
            PrimaryButton(
                text = if (allowedTime > 0) "앱 사용하러 가기" else "이번에 참아볼게",
                onClick = if (allowedTime > 0) onUseApp else onGiveUp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BottomInputPanel(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onAsk: () -> Unit,
    onGiveUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                color = AppTextStrong,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                placeholder = { Text("여기에 이유를 입력해줘") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface,
                    focusedBorderColor = AppBorderStrong,
                    unfocusedBorderColor = AppBorder,
                ),
                minLines = 5,
            )
            PrimaryButton(
                text = "레서판다에게 물어보기",
                onClick = onAsk,
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "이번에 참아볼게",
                onClick = onGiveUp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SpeechCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    SpeechBubbleCard(
        title = title,
        text = subtitle.orEmpty(),
        modifier = modifier.width(270.dp),
    )
}

@Composable
private fun PandaImage(
    size: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.red_panda),
        contentDescription = "레서판다",
        modifier = modifier.size(size.dp),
    )
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeechCard(title = "앗", subtitle = message)
        Spacer(Modifier.height(18.dp))
        PrimaryButton("다시 시도", onRetry, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton("이번에 참아볼게", onHome, modifier = Modifier.fillMaxWidth())
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpeechCard(title = "앗", subtitle = "제한 설정을 찾지 못했어요.")
            Spacer(Modifier.height(18.dp))
            PrimaryButton("닫기", onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun scheduleTimeExpiredIntervention(
    appContext: Context,
    container: AILockContainer,
    session: ActiveUseSession,
) {
    backgroundScope.launch {
        val waitMs = (session.expectedEndAt - System.currentTimeMillis()).coerceAtLeast(0L)
        delay(waitMs)
        val latest = container.activeUseSessionRepository.get(session.packageName) ?: return@launch
        if (latest.state != SESSION_STATE_IN_USE) return@launch
        if (System.currentTimeMillis() < latest.expectedEndAt) return@launch
        OverlayService.show(appContext, session.packageName, timeLimitExceeded = true)
    }
}

private suspend fun closeSession(container: AILockContainer, packageName: String) {
    container.pendingFinalDecisionRepository.clear(packageName)
    container.activeUseSessionRepository.clear(packageName)
}

private fun remainingInputMinutes(dailyLimitMinutes: Int?, session: ActiveUseSession?): Int {
    val sessionRemaining = session?.let {
        ((it.expectedEndAt - System.currentTimeMillis()) / 60_000L).toInt().coerceAtLeast(0)
    }
    return sessionRemaining ?: (dailyLimitMinutes ?: 0).coerceAtLeast(0)
}

private fun minutesLabel(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val mins = safeMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}시간 ${mins}분"
        hours > 0 -> "${hours}시간"
        else -> "${mins}분"
    }
}

private fun formatTimer(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    return "00 : ${safeMinutes.toString().padStart(2, '0')} : 00"
}


