package com.ccomet.ailock.service

import android.animation.ValueAnimator
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
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.ActiveUseSession
import com.ccomet.ailock.data.model.JudgePostRequest
import com.ccomet.ailock.data.model.JudgePreRequest
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.PendingFinalDecision
import com.ccomet.ailock.data.model.PreviousUnlockRequest
import com.ccomet.ailock.data.model.UsageEventType
import com.ccomet.ailock.data.work.SessionWorkScheduler
import com.ccomet.ailock.ui.intervention.InterventionActivity
import com.ccomet.ailock.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
        private var promptStack: View? = null
        private var lastInputReason: String = ""
        private var judgeJob: Job? = null
        private var keyboardTranslationY = 0f
        private var keyboardWasVisible = false
        private var lastKeyboardInsetBottom = 0
        private var onKeyboardShowStarted: (() -> Unit)? = null
        private var onKeyboardDismissStarted: (() -> Unit)? = null

        fun build(): View {
            root = FrameLayout(context).apply {
                setBackgroundColor(Color.argb(0, 0, 0, 0))
                isClickable = true
                isFocusable = true
            }
            if (pending != null && session == null && !timeLimitExceeded) {
                pending = null
                scope.launch {
                    container.pendingFinalDecisionRepository.clear(config.packageName)
                }
            }
            ValueAnimator.ofInt(0, 208).apply {
                duration = 220L
                addUpdateListener { animator ->
                    root.setBackgroundColor(Color.argb(animator.animatedValue as Int, 0, 0, 0))
                }
                start()
            }
            renderInput(animate = true)
            installKeyboardFollower()
            return root
        }

        private fun renderInput(animate: Boolean) {
            root.removeAllViews()
            promptStack = null
            onKeyboardShowStarted = null
            onKeyboardDismissStarted = null
            val isRetry = pending != null
            var expanded = false

            val speech = speechCard(
                title = if (isRetry) "다시 말해볼래?" else "뭐야, ${config.appName} 켰어?",
            )
            val panda = pandaView()
            val prompt = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                alpha = if (animate) 0f else 1f
                addView(speech)
                addView(panda, LinearLayout.LayoutParams(104.dp(), 104.dp()))
            }
            val promptParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ).apply {
                leftMargin = 28.dp()
                rightMargin = 28.dp()
                bottomMargin = 96.dp()
            }
            root.addView(prompt, promptParams)
            promptStack = prompt

            val handle = View(context).apply {
                background = rounded(APP_BORDER_STRONG, 1.dp(), APP_BORDER_STRONG, 0)
            }
            lateinit var updateExpanded: (Boolean) -> Unit
            var inputHeightAnimator: ValueAnimator? = null
            var promptMarginAnimator: ValueAnimator? = null
            val input = object : EditText(context) {
                override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        showSoftInputOnFocus = false
                        if (text.isNullOrBlank()) {
                            updateExpanded(false)
                        }
                    }
                    return super.onKeyPreIme(keyCode, event)
                }
            }.apply {
                hint = "레서판다에게 물어보기"
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(APP_TEXT_STRONG)
                setHintTextColor(APP_TEXT_SUBTLE)
                setPadding(12.dp(), 0, 56.dp(), 0)
                background = rounded(APP_BACKGROUND, 8.dp(), Color.TRANSPARENT, 0)
                showSoftInputOnFocus = false
            }
            val action = ImageView(context).apply {
                setImageResource(R.drawable.ic_action_close)
                setColorFilter(APP_TEXT_STRONG)
                scaleType = ImageView.ScaleType.CENTER
                setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
                contentDescription = "닫기"
                background = rounded(Color.TRANSPARENT, 8.dp(), Color.TRANSPARENT, 0)
                isClickable = true
                isFocusable = true
            }
            val inputRow = FrameLayout(context).apply {
                background = rounded(APP_BACKGROUND, 8.dp(), Color.TRANSPARENT, 0)
                addView(input, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 52.dp(), Gravity.CENTER))
                addView(action, FrameLayout.LayoutParams(46.dp(), 46.dp(), Gravity.END or Gravity.BOTTOM))
            }
            val panel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18.dp(), 10.dp(), 12.dp(), 10.dp())
                background = rounded(APP_BACKGROUND, 8.dp(), APP_BORDER, 1.dp())
                addView(handle, LinearLayout.LayoutParams(24.dp(), 2.dp()).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = 8.dp()
                })
                addView(inputRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
            val panelStack = FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false
                setPadding(28.dp(), 0, 28.dp(), 22.dp())
                addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            }
            bottomStack = panelStack
            root.addView(panelStack, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

            updateExpanded = { nextExpanded: Boolean ->
                if (expanded != nextExpanded) {
                    expanded = nextExpanded
                    input.hint = if (expanded) "왜 지금 ${remainingInputMinutes().coerceAtLeast(1)}분이 필요해?" else "레서판다에게 물어보기"
                    input.gravity = if (expanded) Gravity.TOP else Gravity.CENTER_VERTICAL
                    input.setSingleLine(!expanded)
                    input.minLines = if (expanded) 3 else 1
                    input.maxLines = if (expanded) 4 else 1
                    input.setPadding(12.dp(), if (expanded) 12.dp() else 0, 56.dp(), if (expanded) 42.dp() else 0)
                    val inputParams = input.layoutParams as FrameLayout.LayoutParams
                    inputHeightAnimator?.cancel()
                    inputHeightAnimator = ValueAnimator.ofInt(inputParams.height, if (expanded) 124.dp() else 52.dp()).apply {
                        duration = 180L
                        interpolator = DecelerateInterpolator()
                        addUpdateListener {
                            inputParams.height = it.animatedValue as Int
                            input.layoutParams = inputParams
                        }
                        start()
                    }
                    val actionParams = action.layoutParams as FrameLayout.LayoutParams
                    actionParams.gravity = Gravity.END or Gravity.BOTTOM
                    action.layoutParams = actionParams
                    updateInputActionStyle(action, input.text?.isNotBlank() == true)
                    panel.setPadding(18.dp(), if (expanded) 12.dp() else 10.dp(), 12.dp(), 10.dp())
                    (speech.getChildAt(0) as? LinearLayout)?.let { card ->
                        (card.getChildAt(0) as? TextView)?.text = if (expanded) "이유나 들어보자" else "뭐야, ${config.appName} 켰어?"
                    }
                    val pandaParams = panda.layoutParams as LinearLayout.LayoutParams
                    pandaParams.width = if (expanded) 116.dp() else 104.dp()
                    pandaParams.height = if (expanded) 116.dp() else 104.dp()
                    panda.layoutParams = pandaParams
                    val nextBottom = if (expanded) 176.dp() else 96.dp()
                    val startBottom = promptParams.bottomMargin
                    promptMarginAnimator?.cancel()
                    promptMarginAnimator = ValueAnimator.ofInt(startBottom, nextBottom).apply {
                        duration = 180L
                        interpolator = DecelerateInterpolator()
                        addUpdateListener {
                            promptParams.bottomMargin = it.animatedValue as Int
                            prompt.layoutParams = promptParams
                        }
                        start()
                    }
                }
            }

            input.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    input.showSoftInputOnFocus = false
                    if (input.text.isNullOrBlank()) {
                        updateExpanded(false)
                    }
                }
            }
            onKeyboardShowStarted = {
                updateExpanded(true)
            }
            onKeyboardDismissStarted = {
                input.showSoftInputOnFocus = false
                if (input.text.isNullOrBlank()) {
                    updateExpanded(false)
                }
            }
            input.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    input.showSoftInputOnFocus = true
                    updateExpanded(true)
                    input.requestFocus()
                }
                false
            }
            input.setOnClickListener {
                input.showSoftInputOnFocus = true
                updateExpanded(true)
                input.requestFocus()
                input.post {
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                        ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val hasText = !s.isNullOrBlank()
                    action.setImageResource(if (hasText) R.drawable.ic_action_chevron_right else R.drawable.ic_action_close)
                    action.contentDescription = if (hasText) "보내기" else "닫기"
                    updateInputActionStyle(action, hasText)
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            action.setOnClickListener {
                val value = input.text?.toString()?.trim().orEmpty()
                if (value.isBlank()) {
                    dismissAndHome()
                } else {
                    lastInputReason = value
                    renderLoading()
                    judgeJob?.cancel()
                    judgeJob = scope.launch {
                        runCatching {
                            judgeUnlock(value)
                        }.onSuccess {
                            pending = it
                            renderResult(it)
                        }.onFailure {
                            if (it !is CancellationException) {
                                renderError("판단 중 문제가 생겼어요. 다시 시도해 주세요.")
                            }
                        }
                    }
                }
            }

            if (!animate) return

            panelStack.translationY = PANEL_ENTER_OFFSET.dp().toFloat()
            panelStack.animate()
                .translationY(0f)
                .setDuration(150)
                .setInterpolator(OvershootInterpolator(0.9f))
                .start()

            prompt.animate()
                .alpha(1f)
                .setDuration(170)
                .setStartDelay(70L)
                .setInterpolator(DecelerateInterpolator())
                .start()

            input.postDelayed({
                input.requestFocus()
            }, 220L)
        }
        private suspend fun judgeUnlock(input: String): PendingFinalDecision {
            val dailyLimit = dailyLimitMinutes()
            val todayUsage = todayUsageMinutes()
            container.ailockRepository.recordEvent(
                packageName = config.packageName,
                appName = config.appName,
                eventType = UsageEventType.AI_REQUEST,
                userInput = input,
                lockReason = config.lockReasonFinal.ifBlank { config.lockReasonCustom },
            )

            val currentSession = session ?: container.activeUseSessionRepository.get(config.packageName)
                ?.also { session = it }
            val decision = if (timeLimitExceeded && currentSession != null) {
                val post = container.ollamaDecisionRepository.judgePost(
                    JudgePostRequest(
                        sessionId = currentSession.sessionId,
                        appName = config.appName,
                        previousReason = currentSession.preInput,
                        postInput = input,
                        requestCount = 1,
                        todayAppUsageMinutes = todayUsage,
                        dailyLimitMinutes = dailyLimit,
                    ),
                )
                PendingFinalDecision(
                    sessionId = currentSession.sessionId,
                    status = post.status,
                    allowedTime = post.allowedTime,
                    supportMessage = post.supportMessage,
                    userInput = input,
                    stateScore = post.stateScore,
                    finalDecision = post.finalDecision,
                )
            } else {
                val pre = container.ollamaDecisionRepository.judgePre(
                    JudgePreRequest(
                        appName = config.appName,
                        preInput = input,
                        requestUseTime = requestedMinutes(dailyLimit, todayUsage),
                        todayAppUsageMinutes = todayUsage,
                        dailyLimitMinutes = dailyLimit,
                        previousRequest = pending?.toPreviousRequest(),
                    ),
                )
                PendingFinalDecision(
                    sessionId = pre.sessionId,
                    status = pre.status,
                    allowedTime = pre.allowedTime,
                    supportMessage = pre.supportMessage,
                    userInput = input,
                    stateScore = pre.stateScore,
                    finalDecision = pre.finalDecision,
                    reasonForDecision = pre.reason,
                )
            }

            recordAiResult(input, decision)
            return decision.also {
                pending = it
                if (it.allowedTime > 0 && currentSession == null) {
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
            promptStack = null
            onKeyboardShowStarted = null
            onKeyboardDismissStarted = null
            val prompt = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(speechCard("흠..."))
                addView(pandaView(), LinearLayout.LayoutParams(116.dp(), 116.dp()))
            }
            root.addView(
                prompt,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                    leftMargin = 28.dp()
                    rightMargin = 28.dp()
                bottomMargin = 132.dp()
                },
            )
            promptStack = prompt

            val panel = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(18.dp(), 14.dp(), 12.dp(), 14.dp())
                background = rounded(APP_SURFACE_MUTED, 8.dp(), APP_BORDER, 1.dp())
                addView(
                    TextView(context).apply {
                        text = "…"
                        textSize = 24f
                        gravity = Gravity.CENTER
                        setTextColor(PANDA_ORANGE)
                    },
                    LinearLayout.LayoutParams(30.dp(), LinearLayout.LayoutParams.WRAP_CONTENT),
                )
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(context).apply {
                            text = "레서판다가 생각하고 있어요..."
                            textSize = 15f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(APP_TEXT_STRONG)
                        })
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                addView(
                    ImageView(context).apply {
                        setImageResource(R.drawable.ic_action_close)
                        setColorFilter(APP_TEXT_STRONG)
                        scaleType = ImageView.ScaleType.CENTER
                        setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
                        contentDescription = "취소"
                        background = rounded(Color.TRANSPARENT, 8.dp(), APP_BORDER_STRONG, 1.dp())
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            judgeJob?.cancel()
                            judgeJob = null
                            renderInput(animate = true)
                        }
                    },
                    LinearLayout.LayoutParams(46.dp(), 46.dp()).apply { leftMargin = 10.dp() },
                )
            }
            val stack = FrameLayout(context).apply {
                setPadding(28.dp(), 0, 28.dp(), 22.dp())
                addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            }
            bottomStack = stack
            root.addView(stack, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            stack.translationY = PANEL_ENTER_OFFSET.dp().toFloat()
            stack.animate().translationY(0f).setDuration(150).setInterpolator(DecelerateInterpolator()).start()
        }
        private fun renderResult(decision: PendingFinalDecision) {
            root.removeAllViews()
            bottomStack = null
            promptStack = null
            onKeyboardShowStarted = null
            onKeyboardDismissStarted = null
            val allowedTime = decision.allowedTime
            val allowed = allowedTime > 0
            val message = decision.supportMessage?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: if (allowed) "필요한 것만 확인하고 바로 돌아와." else "지금은 멈추는 쪽이 더 좋아 보여."

            val prompt = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(speechCard(if (allowed) "좋아, ${allowedTime}분만" else message))
                addView(pandaView(), LinearLayout.LayoutParams(if (allowed) 124.dp() else 116.dp(), if (allowed) 124.dp() else 116.dp()))
            }
            root.addView(
                prompt,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                    leftMargin = 28.dp()
                    rightMargin = 28.dp()
                    bottomMargin = 216.dp()
                },
            )
            promptStack = prompt

            val panel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18.dp(), 18.dp(), 18.dp(), 18.dp())
                background = rounded(APP_SURFACE_MUTED, 8.dp(), APP_BORDER, 1.dp())
                if (allowed) {
                    addView(TextView(context).apply {
                        text = "${allowedTime}분 허용됐어요"
                        textSize = 18f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(APP_TEXT_STRONG)
                    })
                    addView(TextView(context).apply {
                        text = "${config.appName}에서 필요한 일만 끝내고 돌아와요. $message"
                        textSize = 14f
                        setTextColor(APP_TEXT_SUBTLE)
                        setPadding(0, 8.dp(), 0, 0)
                        maxLines = 4
                    })
                }
                addView(
                    actionButton(if (allowed) "앱으로 돌아가기" else "홈으로 가기") {
                        if (allowed) allowAndDismiss(decision) else dismissAndHome()
                    },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp()).apply { topMargin = 14.dp() },
                )
                addView(
                    actionButton(if (allowed) "이번엔 참아볼게" else "다시 말해볼게", primary = false) {
                        if (allowed) dismissAndHome() else renderInput(animate = true)
                    },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp()).apply { topMargin = 10.dp() },
                )
            }
            val stack = FrameLayout(context).apply {
                setPadding(22.dp(), 0, 22.dp(), 22.dp())
                addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            }
            bottomStack = stack
            root.addView(stack, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
            stack.translationY = PANEL_ENTER_OFFSET.dp().toFloat()
            stack.animate().translationY(0f).setDuration(150).setInterpolator(DecelerateInterpolator()).start()
        }
        private fun renderError(message: String) {
            root.removeAllViews()
            bottomStack = null
            promptStack = null
            onKeyboardShowStarted = null
            onKeyboardDismissStarted = null
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

        private fun updateInputActionStyle(action: ImageView, hasText: Boolean) {
            action.background = rounded(
                if (hasText) PANDA_ORANGE else Color.TRANSPARENT,
                8.dp(),
                Color.TRANSPARENT,
                0,
            )
            action.setColorFilter(if (hasText) Color.WHITE else APP_TEXT_STRONG)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                root.setWindowInsetsAnimationCallback(
                    object : WindowInsetsAnimation.Callback(WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                        override fun onProgress(
                            insets: WindowInsets,
                            runningAnimations: MutableList<WindowInsetsAnimation>,
                        ): WindowInsets {
                            applyImeTranslation(insets, animate = false)
                            return insets
                        }

                        override fun onEnd(animation: WindowInsetsAnimation) {
                            root.rootWindowInsets?.let { applyImeTranslation(it, animate = false) }
                        }
                    },
                )
                root.setOnApplyWindowInsetsListener { _, insets ->
                    applyImeTranslation(insets, animate = false)
                    insets
                }
                root.post {
                    root.requestApplyInsets()
                }
            } else {
                root.viewTreeObserver.addOnGlobalLayoutListener {
                    applyVisibleFrameTranslation(animate = false)
                }
                root.post {
                    applyVisibleFrameTranslation(animate = false)
                }
            }
        }

        private fun applyImeTranslation(insets: WindowInsets, animate: Boolean) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
            val stack = bottomStack ?: return
            if (root.height == 0) return
            val imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom
            val isImeVisible = insets.isVisible(WindowInsets.Type.ime())
            val isKeyboardVisible = isImeVisible && imeBottom > 0
            if (!keyboardWasVisible && isKeyboardVisible) {
                onKeyboardShowStarted?.invoke()
            }
            if (keyboardWasVisible && imeBottom < lastKeyboardInsetBottom) {
                onKeyboardDismissStarted?.invoke()
            }
            val target = if (isImeVisible && imeBottom > 0) {
                translationForVisibleBottom(root.height - imeBottom, stack)
            } else {
                0f
            }
            keyboardWasVisible = isKeyboardVisible
            lastKeyboardInsetBottom = imeBottom
            applyOverlayTranslation(target, animate)
        }

        private fun applyVisibleFrameTranslation(animate: Boolean) {
            val rect = Rect()
            root.getWindowVisibleDisplayFrame(rect)
            val stack = bottomStack ?: return
            if (root.height == 0) return
            val rootLocation = IntArray(2)
            root.getLocationOnScreen(rootLocation)
            val visibleBottom = rect.bottom - rootLocation[1]
            val target = translationForVisibleBottom(visibleBottom, stack)
            val isKeyboardVisible = target < -1f
            if (!keyboardWasVisible && isKeyboardVisible) {
                onKeyboardShowStarted?.invoke()
            }
            if (keyboardWasVisible && target > keyboardTranslationY + 1f) {
                onKeyboardDismissStarted?.invoke()
            }
            keyboardWasVisible = isKeyboardVisible
            applyOverlayTranslation(target, animate)
        }

        private fun translationForVisibleBottom(visibleBottom: Int, stack: FrameLayout): Float {
            val stackBottom = root.height - stack.paddingBottom
            return (visibleBottom - KEYBOARD_TOP_GAP.dp() - stackBottom).coerceAtMost(0).toFloat()
        }

        private fun applyOverlayTranslation(target: Float, animate: Boolean) {
            if (target == 0f && bottomStack?.translationY?.let { it > 0f } == true) {
                keyboardTranslationY = target
                return
            }
            if (abs(keyboardTranslationY - target) < 1f && animate) return
            keyboardTranslationY = target
            listOfNotNull(bottomStack, promptStack).forEach { view ->
                if (target == 0f && view.translationY > 0f) return@forEach
                if (abs(view.translationY - target) < 1f) return@forEach
                view.animate().cancel()
                if (animate) {
                    view.animate()
                        .translationY(target)
                        .setDuration(180L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                } else {
                    view.translationY = target
                }
            }
        }

        private fun PendingFinalDecision.toPreviousRequest(): PreviousUnlockRequest =
            PreviousUnlockRequest(
                userUnlockReason = userInput,
                previousStateScore = stateScore.takeIf { it > 0f } ?: 2.0f,
                previousUserStateLevel = status ?: "CAUTION",
                finalDecision = finalDecision,
            )

        private suspend fun recordAiResult(input: String, decision: PendingFinalDecision) {
            container.ailockRepository.recordEvent(
                packageName = config.packageName,
                appName = config.appName,
                eventType = if (decision.allowedTime > 0) UsageEventType.AI_ALLOWED else UsageEventType.AI_DENIED,
                aiStatus = decision.status,
                aiAllowedTime = decision.allowedTime,
                userInput = input,
                lockReason = decision.supportMessage ?: decision.reasonForDecision,
            )
        }

        private fun requestedMinutes(dailyLimit: Int, todayUsage: Int): Int {
            val remaining = (dailyLimit - todayUsage).coerceAtLeast(1)
            return remaining.coerceAtMost(DEFAULT_ALLOW_MINUTES)
        }

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
    private const val KEYBOARD_TOP_GAP = 20
    private const val PANEL_ENTER_OFFSET = 140
    private const val PANEL_SPACE = 350
    private val APP_BACKGROUND = Color.rgb(255, 251, 245)
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


