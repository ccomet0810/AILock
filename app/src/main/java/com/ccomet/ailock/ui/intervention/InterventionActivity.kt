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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockTheme
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppBorderStrong
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaOrange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private const val SESSION_STATE_IN_USE = "IN_USE"

class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
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
    var requestJob by remember { mutableStateOf<Job?>(null) }
    var overlayVisible by remember { mutableStateOf(false) }

    LaunchedEffect(packageName, timeLimitExceeded) {
        val session = container.activeUseSessionRepository.get(packageName)
        val pending = container.pendingFinalDecisionRepository.get(packageName)
        activeSession = session
        pendingDecision = pending
        isAdditionalRequest = session != null && (timeLimitExceeded || System.currentTimeMillis() >= session.expectedEndAt)
        screenState = if (pending != null) InterventionScreenState.RESULT else InterventionScreenState.INPUT
    }

    LaunchedEffect(Unit) {
        overlayVisible = true
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (overlayVisible) 0.82f else 0f,
        animationSpec = tween(220),
        label = "intervention-overlay-alpha",
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black.copy(alpha = overlayAlpha))
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
                        requestJob?.cancel()
                        screenState = InterventionScreenState.LOADING
                        requestJob = scope.launch {
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
                            }.onFailure { throwable ->
                                if (throwable !is CancellationException) {
                                    errorMessage = "판단 중 문제가 생겼어요. 다시 시도해 주세요."
                                    screenState = InterventionScreenState.ERROR
                                }
                            }
                        }
                    },
                    onGiveUp = onHome,
                    modifier = Modifier.fillMaxSize(),
                )

                InterventionScreenState.LOADING -> LoadingScreen(
                    onCancel = {
                        requestJob?.cancel()
                        requestJob = null
                        screenState = InterventionScreenState.INPUT
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                InterventionScreenState.RESULT -> ResultScreen(
                    appName = config.appName,
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
                    onRetry = {
                        pendingDecision = null
                        screenState = InterventionScreenState.INPUT
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
    var focused by remember { mutableStateOf(false) }
    var panelVisible by remember { mutableStateOf(false) }
    val expanded = focused || value.isNotBlank()
    val speech = when {
        isAdditionalRequest -> "시간 더 필요해?"
        expanded -> "이유나 들어보자"
        else -> "뭐야, ${appName} 켰어?"
    }
    val sheetHeight by animateDpAsState(
        targetValue = if (expanded) 176.dp else 72.dp,
        animationSpec = tween(180),
        label = "reason-sheet-height",
    )
    val pandaBottom by animateDpAsState(
        targetValue = if (expanded) sheetHeight + 46.dp else sheetHeight - 36.dp,
        animationSpec = tween(190),
        label = "panda-peek-bottom",
    )

    LaunchedEffect(Unit) {
        panelVisible = true
    }

    Box(modifier = modifier) {
        PandaPrompt(
            text = speech,
            pandaSize = if (expanded) 116 else 104,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = pandaBottom),
        )

        AnimatedVisibility(
            visible = panelVisible,
            enter = slideInVertically(animationSpec = tween(180), initialOffsetY = { it / 2 }) + fadeIn(tween(140)),
            exit = slideOutVertically(animationSpec = tween(140), targetOffsetY = { it / 2 }) + fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReasonInputCard(
                value = value,
                expanded = expanded,
                remainingMinutes = remainingMinutes,
                onValueChange = onValueChange,
                onFocusChange = { focused = it },
                onSubmit = onAsk,
                onClose = onGiveUp,
            )
        }
    }
}
@Composable
private fun LoadingScreen(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PandaPrompt(
            text = "흠...",
            pandaSize = 116,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 132.dp),
        )
        StatusBottomCard(
            title = "레서판다가 생각하고 있어요...",
            body = "이유를 살펴보고 잠깐 허용해도 되는지 판단하는 중이에요.",
            action = {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "취소")
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
@Composable
private fun ResultScreen(
    appName: String,
    decision: PendingFinalDecision?,
    onUseApp: () -> Unit,
    onGiveUp: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allowedTime = decision?.allowedTime ?: 0
    val allowed = allowedTime > 0
    val message = decision?.supportMessage?.lineSequence()?.firstOrNull()?.ifBlank { null }
        ?: if (allowed) "필요한 것만 확인하고 바로 돌아와." else "지금은 멈추는 쪽이 더 좋아 보여."
    val speech = if (allowed) "좋아, ${allowedTime}분만" else "이번엔 멈추자"

    Box(modifier = modifier) {
        PandaPrompt(
            text = speech,
            pandaSize = if (allowed) 124 else 116,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 216.dp),
        )
        DecisionBottomCard(
            title = if (allowed) "${allowedTime}분 허용됐어요" else "허용하지 않았어요",
            body = if (allowed) {
                "${appName}에서 필요한 일만 끝내고 돌아와요. $message"
            } else {
                message
            },
            primaryText = if (allowed) "앱으로 돌아가기" else "홈으로 가기",
            secondaryText = if (allowed) "이번엔 참아볼게" else "다시 말해볼게",
            onPrimary = if (allowed) onUseApp else onGiveUp,
            onSecondary = if (allowed) onGiveUp else onRetry,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
@Composable
private fun ReasonInputCard(
    value: String,
    expanded: Boolean,
    remainingMinutes: Int,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 22.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
        shape = AILockShape.card,
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (expanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(24.dp)
                        .height(2.dp)
                        .clip(AILockShape.pill)
                        .background(AppBorderStrong),
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (expanded) 124.dp else 52.dp)
                    .onFocusChanged { onFocusChange(it.isFocused) },
                placeholder = {
                    Text(if (expanded) "왜 지금 ${remainingMinutes.coerceAtLeast(1)}분이 필요해?" else "레서판다에게 물어보기")
                },
                trailingIcon = {
                    IconButton(onClick = if (value.isBlank()) onClose else onSubmit) {
                        Icon(
                            imageVector = if (value.isBlank()) Icons.Default.Close else Icons.Default.ArrowForward,
                            contentDescription = if (value.isBlank()) "닫기" else "보내기",
                        )
                    }
                },
                shape = AILockShape.control,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface,
                    focusedBorderColor = AppBorderStrong,
                    unfocusedBorderColor = AppBorder,
                    cursorColor = PandaOrange,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (value.isNotBlank()) onSubmit() }),
                singleLine = !expanded,
                minLines = if (expanded) 3 else 1,
                maxLines = if (expanded) 4 else 1,
            )
        }
    }
}

@Composable
private fun StatusBottomCard(
    title: String,
    body: String,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(animationSpec = tween(170), initialOffsetY = { it / 2 }) + fadeIn(tween(120)),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 22.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
            shape = AILockShape.card,
            border = BorderStroke(1.dp, AppBorder),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = PandaOrange,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = AppTextStrong)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
                }
                action()
            }
        }
    }
}

@Composable
private fun DecisionBottomCard(
    title: String,
    body: String,
    primaryText: String,
    secondaryText: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 22.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
        shape = AILockShape.card,
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppTextStrong)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = AppTextSubtle)
            PrimaryButton(primaryText, onPrimary, modifier = Modifier.fillMaxWidth())
            SecondaryButton(secondaryText, onSecondary, modifier = Modifier.fillMaxWidth())
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
private fun PandaPrompt(
    text: String,
    pandaSize: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeechBubbleCard(
            title = text,
            text = "",
            modifier = Modifier.width(176.dp),
        )
        PandaImage(size = pandaSize)
    }
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


