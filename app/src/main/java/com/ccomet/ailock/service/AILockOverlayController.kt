package com.ccomet.ailock.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.ActiveUseSession
import com.ccomet.ailock.data.model.JudgePreRequest
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.PendingFinalDecision
import com.ccomet.ailock.data.model.PreviousUnlockRequest
import com.ccomet.ailock.data.work.SessionWorkScheduler
import com.ccomet.ailock.ui.intervention.InterventionActivity
import com.ccomet.ailock.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.abs

object AILockOverlayController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentViewRef: WeakReference<View>? = null
    private var currentPackageName: String? = null
    private var openingPackageName: String? = null

    fun show(
        context: Context,
        config: LockedAppConfig,
        timeLimitExceeded: Boolean,
        initialSession: ActiveUseSession? = null,
        initialPending: PendingFinalDecision? = null,
    ) {
        val appContext = context.applicationContext
        if (isShowingFor(config.packageName)) return

        if (!Settings.canDrawOverlays(appContext)) {
            appContext.startActivity(
                InterventionActivity.intent(appContext, config.packageName, timeLimitExceeded)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        openingPackageName = config.packageName
        val container = AILockContainer.get(appContext)
        val overlay = OverlayUi(
            context = appContext,
            container = container,
            config = config,
            timeLimitExceeded = timeLimitExceeded,
            initialSession = initialSession,
            initialPending = initialPending,
        )
        val view = overlay.build()
        val windowManager = appContext.getSystemService(WindowManager::class.java)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            @Suppress("DEPRECATION")
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = 28
            }
        }

        runCatching {
            currentViewRef = WeakReference(view)
            currentPackageName = config.packageName
            windowManager.addView(view, params)
        }.onFailure {
            clearShowingState(config.packageName)
            appContext.startActivity(
                InterventionActivity.intent(appContext, config.packageName, timeLimitExceeded)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }.onSuccess {
            openingPackageName = null
        }

    }

    fun dismiss(context: Context) {
        val view = currentViewRef?.get() ?: run {
            openingPackageName = null
            currentViewRef = null
            currentPackageName = null
            return
        }
        runCatching {
            context.applicationContext.getSystemService(WindowManager::class.java).removeViewImmediate(view)
        }
        clearShowingState(currentPackageName)
        AILockAccessibilityService.resetOverlayCooldown()
    }

    fun isShowingFor(packageName: String): Boolean =
        (currentViewRef?.get() != null && currentPackageName == packageName) || openingPackageName == packageName

    fun showingPackageName(): String? =
        currentPackageName ?: openingPackageName

    private fun clearShowingState(packageName: String?) {
        if (currentPackageName == packageName) {
            currentViewRef = null
            currentPackageName = null
        }
        if (openingPackageName == packageName) {
            openingPackageName = null
        }
    }

    private class OverlayUi(
        private val context: Context,
        private val container: AILockContainer,
        private val config: LockedAppConfig,
        private val timeLimitExceeded: Boolean,
        initialSession: ActiveUseSession?,
        initialPending: PendingFinalDecision?,
    ) {
        private val density = context.resources.displayMetrics.density
        private lateinit var root: FrameLayout
        private var session: ActiveUseSession? = initialSession
        private var pending: PendingFinalDecision? = initialPending
        private var bottomStack: FrameLayout? = null
        private var lastInputReason: String = ""

        fun build(): View {
            root = FrameLayout(context).apply {
                setBackgroundColor(Color.argb(190, 0, 0, 0))
                isClickable = true
                isFocusable = true
            }
            if (pending != null) {
                renderResult(pending!!)
            } else {
                renderInput(animate = true)
            }
            installKeyboardFollower()
            return root
        }

        private fun renderInput(animate: Boolean) {
            root.removeAllViews()
            val isRetry = pending != null

            val panel = bottomPanel(
                title = if (isRetry) "다시 말해봐" else "왜 켰냐?",
                hint = "여기에 이유를 입력해줘",
                primary = "레서판다에게 물어보기",
                secondary = "이번에 참아볼게",
                onPrimary = { input ->
                    if (input.isBlank()) return@bottomPanel
                    lastInputReason = input
                    renderLoading()
                    scope.launch {
                        runCatching {
                            judgeUnlock(input)
                        }.onSuccess {
                            pending = it
                            if (it.allowedTime <= 0) {
                                container.pendingFinalDecisionRepository.save(config.packageName, it)
                            }
                            renderResult(it)
                        }.onFailure {
                            renderError("판단 중 문제가 생겼어요. 다시 시도해 주세요.")
                        }
                    }
                },
                onSecondary = { dismissAndHome() },
            )

            val visual = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(28.dp(), 24.dp(), 28.dp(), 18.dp())
                alpha = if (animate) 0f else 1f
                translationY = if (animate) 12.dp().toFloat() else 0f
            }
            visual.addView(
                speechCard(
                    title = config.appName.uppercase(),
                    subtitle = "남은 시간 ${minutesLabel(remainingInputMinutes())}",
                ),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            visual.addView(
                pandaView(),
                LinearLayout.LayoutParams(132.dp(), 132.dp()).apply { topMargin = 12.dp() },
            )
            root.addView(
                visual,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                    bottomMargin = PANEL_SPACE.dp()
                },
            )

            val panelStack = FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false
                addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            }
            bottomStack = panelStack
            root.addView(panelStack, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

            if (!animate) return

            panelStack.translationY = PANEL_ENTER_OFFSET.dp().toFloat()
            panelStack.animate()
                .translationY(0f)
                .setDuration(110)
                .setInterpolator(DecelerateInterpolator())
                .start()

            visual.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(160)
                .setStartDelay(70L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        private suspend fun judgeUnlock(input: String): PendingFinalDecision {
            val dailyLimit = dailyLimitMinutes()
            val pre = container.ollamaDecisionRepository.judgePre(
                JudgePreRequest(
                    appName = config.appName,
                    preInput = input,
                    requestUseTime = DEFAULT_ALLOW_MINUTES,
                    todayAppUsageMinutes = todayUsageMinutes(),
                    dailyLimitMinutes = dailyLimit,
                    previousRequest = pending?.toPreviousRequest(),
                ),
            )
            return PendingFinalDecision(
                sessionId = pre.sessionId,
                status = pre.status,
                allowedTime = pre.allowedTime,
                supportMessage = pre.supportMessage,
                userInput = input,
                stateScore = pre.stateScore,
                finalDecision = pre.finalDecision,
                reasonForDecision = pre.reason,
            ).also {
                if (it.allowedTime > 0) {
                    val now = System.currentTimeMillis()
                    session = ActiveUseSession(
                        packageName = config.packageName,
                        appName = config.appName,
                        sessionId = it.sessionId,
                        preInput = input,
                        plannedMinutes = it.allowedTime,
                        startedAt = now,
                        expectedEndAt = now + it.allowedTime * 60_000L,
                    )
                }
            }
        }

        private fun renderLoading() {
            root.removeAllViews()
            bottomStack = null
            val center = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(28.dp(), 0, 28.dp(), 0)
            }
            center.addView(speechCard("흠........"))
            center.addView(pandaView(), LinearLayout.LayoutParams(128.dp(), 128.dp()).apply { topMargin = 18.dp() })
            root.addView(center, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            val bottom = TextView(context).apply {
                text = context.getString(R.string.judgement_loading_message)
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            }
            root.addView(
                bottom,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                    bottomMargin = 38.dp()
                },
            )
        }

        private fun renderResult(decision: PendingFinalDecision) {
            root.removeAllViews()
            bottomStack = null
            val allowedTime = decision.allowedTime
            val message = decision.supportMessage?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: if (allowedTime > 0) "필요한 만큼만 확인하고 바로 돌아와." else "지금은 멈추는 게 좋아."

            val center = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(28.dp(), 0, 28.dp(), 80.dp())
            }
            center.addView(pandaView(), LinearLayout.LayoutParams(142.dp(), 142.dp()))
            center.addView(
                TextView(context).apply {
                    text = message
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setPadding(0, 14.dp(), 0, 0)
                    maxLines = 4
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            root.addView(center, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            val bottom = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp(), 0, 16.dp(), 22.dp())
                gravity = Gravity.CENTER
            }
            bottom.addView(
                TextView(context).apply {
                    text = formatTimer(allowedTime)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            bottom.addView(
                actionButton(if (allowedTime > 0) "앱 사용하러 가기" else "다시 설명할게") {
                    if (allowedTime > 0) allowAndDismiss(decision) else renderInput(animate = true)
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp()).apply { topMargin = 10.dp() },
            )
            if (allowedTime <= 0) {
                bottom.addView(
                    actionButton("이번엔 참아볼게", primary = false) { dismissAndHome() },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp()).apply { topMargin = 10.dp() },
                )
            }
            root.addView(bottom, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        }

        private fun renderError(message: String) {
            root.removeAllViews()
            bottomStack = null
            val panel = bottomPanel(
                title = "앗",
                hint = message,
                primary = "다시 시도",
                secondary = "이번엔 참아볼게",
                onPrimary = { renderInput(animate = true) },
                onSecondary = { dismissAndHome() },
                inputEnabled = false,
            )
            root.addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        }

        private fun bottomPanel(
            title: String,
            hint: String,
            primary: String,
            secondary: String,
            onPrimary: (String) -> Unit,
            onSecondary: () -> Unit,
            inputEnabled: Boolean = true,
        ): LinearLayout {
            val input = EditText(context).apply {
                this.hint = hint
                minLines = 5
                gravity = Gravity.TOP
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isEnabled = inputEnabled
                textSize = 14f
                setTextColor(APP_TEXT_STRONG)
                setHintTextColor(APP_TEXT_SUBTLE)
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
                background = rounded(Color.WHITE, 8.dp(), APP_BORDER_STRONG, 1.dp())
            }

            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24.dp(), 24.dp(), 24.dp(), 28.dp())
                background = rounded(APP_SURFACE_MUTED, 22.dp(), APP_BORDER, 1.dp(), topOnly = true)
                addView(
                    TextView(context).apply {
                        text = title
                        textSize = 18f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(APP_TEXT_STRONG)
                    },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
                )
                addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 148.dp()).apply { topMargin = 12.dp() })
                addView(actionButton(primary, primary = true) { onPrimary(input.text?.toString()?.trim().orEmpty()) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp()).apply { topMargin = 12.dp() })
                addView(actionButton(secondary, primary = false) { onSecondary() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp()).apply { topMargin = 10.dp() })
            }
        }

        private fun speechCard(title: String, subtitle: String? = null): LinearLayout {
            val wrap = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(16.dp(), 18.dp(), 16.dp(), 18.dp())
                background = rounded(Color.WHITE, 8.dp(), APP_BORDER, 1.dp())
            }
            card.addView(TextView(context).apply {
                text = title
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(APP_TEXT_STRONG)
            })
            if (subtitle != null) {
                card.addView(TextView(context).apply {
                    text = subtitle
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTextColor(APP_TEXT_SUBTLE)
                    setPadding(0, 8.dp(), 0, 0)
                })
            }
            wrap.addView(card, LinearLayout.LayoutParams(270.dp(), LinearLayout.LayoutParams.WRAP_CONTENT))
            wrap.addView(
                BubbleTailView(context),
                LinearLayout.LayoutParams(22.dp(), 12.dp()).apply { topMargin = (-1).dp() },
            )
            return wrap
        }

        private fun pandaView(): ImageView =
            ImageView(context).apply {
                setImageResource(R.drawable.red_panda)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

        private fun actionButton(text: String, primary: Boolean = true, onClick: () -> Unit): TextView =
            TextView(context).apply {
                this.text = text
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (primary) Color.WHITE else APP_TEXT_STRONG)
                background = if (primary) {
                    rounded(PANDA_ORANGE, 8.dp(), PANDA_ORANGE, 1.dp())
                } else {
                    rounded(Color.WHITE, 8.dp(), APP_BORDER, 1.dp())
                }
                isClickable = true
                isFocusable = true
                minHeight = 52.dp()
                setOnClickListener { onClick() }
            }

        private fun allowAndDismiss(decision: PendingFinalDecision) {
            val allowedTime = decision.allowedTime
            if (allowedTime <= 0) {
                dismissAndHome()
                return
            }
            scope.launch {
                val now = System.currentTimeMillis()
                val current = session
                val next = current?.copy(
                    expectedEndAt = now + allowedTime * 60_000L,
                    state = SESSION_STATE_IN_USE,
                ) ?: ActiveUseSession(
                    packageName = config.packageName,
                    appName = config.appName,
                    sessionId = decision.sessionId,
                    preInput = lastInputReason,
                    plannedMinutes = allowedTime,
                    startedAt = now,
                    expectedEndAt = now + allowedTime * 60_000L,
                )
                container.activeUseSessionRepository.save(next)
                container.pendingFinalDecisionRepository.clear(config.packageName)
                container.ailockRepository.grantTemporaryAllowance(config.packageName, allowedTime)
                SessionWorkScheduler.scheduleTimeExpiredNudge(context, config.packageName, allowedTime * 60_000L)
                dismiss(context)
            }
        }

        private fun dismissAndHome() {
            scope.launch {
                SessionWorkScheduler.cancelAllForPackage(context, config.packageName)
                container.pendingFinalDecisionRepository.clear(config.packageName)
                container.activeUseSessionRepository.clear(config.packageName)
                dismiss(context)
                if (AILockAccessibilityService.goHomeIfAvailable()) return@launch
                context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }

        private fun installKeyboardFollower() {
            root.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = Rect()
                root.getWindowVisibleDisplayFrame(rect)
                val hiddenHeight = (root.rootView.height - rect.bottom).coerceAtLeast(0)
                val target = if (hiddenHeight > 120.dp()) -hiddenHeight.toFloat() else 0f
                val stack = bottomStack ?: return@addOnGlobalLayoutListener
                if (target == 0f && stack.translationY > 0f) return@addOnGlobalLayoutListener
                if (abs(stack.translationY - target) < 1f) return@addOnGlobalLayoutListener
                stack.animate()
                    ?.translationY(target)
                    ?.setDuration(120)
                    ?.setInterpolator(DecelerateInterpolator())
                    ?.start()
            }
        }

        private fun PendingFinalDecision.toPreviousRequest(): PreviousUnlockRequest =
            PreviousUnlockRequest(
                userUnlockReason = userInput,
                previousStateScore = stateScore.takeIf { it > 0f } ?: 2.0f,
                previousUserStateLevel = status ?: "CAUTION",
                finalDecision = finalDecision,
            )

        private fun remainingInputMinutes(): Int {
            val sessionRemaining = session?.let {
                ((it.expectedEndAt - System.currentTimeMillis()) / 60_000L).toInt().coerceAtLeast(0)
            }
            if (sessionRemaining != null) return sessionRemaining
            return (dailyLimitMinutes() - todayUsageMinutes()).coerceAtLeast(0)
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

        private fun dailyLimitMinutes(): Int {
            val today = TimeUtils.currentDayOfWeek()
            return (config.advancedDayLimits[today] ?: config.dailyLimitMinutes ?: DEFAULT_DAILY_LIMIT_MINUTES)
                .coerceAtLeast(1)
        }

        private fun todayUsageMinutes(): Int {
            val manager = context.getSystemService(UsageStatsManager::class.java)
            val start = TimeUtils.todayStartMillis()
            val end = System.currentTimeMillis()
            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            val total = stats
                ?.filter { it.packageName == config.packageName }
                ?.sumOf { it.totalTimeInForeground }
                ?: 0L
            return (total / 60_000L).toInt()
        }

        private fun rounded(
            color: Int,
            radius: Int,
            strokeColor: Int,
            strokeWidth: Int,
            topOnly: Boolean = false,
        ): GradientDrawable =
            GradientDrawable().apply {
                setColor(color)
                if (topOnly) {
                    cornerRadii = floatArrayOf(
                        radius.toFloat(), radius.toFloat(),
                        radius.toFloat(), radius.toFloat(),
                        0f, 0f,
                        0f, 0f,
                    )
                } else {
                    cornerRadius = radius.toFloat()
                }
                if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
            }

        private fun Int.dp(): Int = (this * density).toInt()
    }

    private class BubbleTailView(context: Context) : View(context) {
        private val path = Path()
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 212, 216)
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            path.apply {
                reset()
                moveTo(0f, 0f)
                lineTo(width / 2f, height.toFloat())
                lineTo(width.toFloat(), 0f)
                close()
            }
            canvas.drawPath(path, fillPaint)
            val halfStroke = strokePaint.strokeWidth / 2f
            canvas.drawLine(halfStroke, 0f, width / 2f, height - halfStroke, strokePaint)
            canvas.drawLine(width - halfStroke, 0f, width / 2f, height - halfStroke, strokePaint)
        }
    }

    private const val SESSION_STATE_IN_USE = "IN_USE"
    private const val DEFAULT_ALLOW_MINUTES = 3
    private const val DEFAULT_DAILY_LIMIT_MINUTES = 120
    private const val PANEL_ENTER_OFFSET = 140
    private const val PANEL_SPACE = 350
    private val APP_SURFACE_MUTED = Color.rgb(255, 242, 227)
    private val APP_BORDER = Color.rgb(225, 214, 202)
    private val APP_BORDER_STRONG = Color.rgb(111, 98, 90)
    private val APP_TEXT_STRONG = Color.rgb(23, 17, 14)
    private val APP_TEXT_SUBTLE = Color.rgb(129, 116, 108)
    private val PANDA_ORANGE = Color.rgb(242, 111, 18)
}

private fun formatTimer(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    return "00 : ${safeMinutes.toString().padStart(2, '0')} : 00"
}


