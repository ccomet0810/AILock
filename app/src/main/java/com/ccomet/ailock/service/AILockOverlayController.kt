package com.ccomet.ailock.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.AiDecisionRequest
import com.ccomet.ailock.data.model.AiDecisionResponse
import com.ccomet.ailock.data.model.AiDecisionStatus
import com.ccomet.ailock.data.model.CurrentStats
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.UsageEventType
import com.ccomet.ailock.ui.intervention.InterventionActivity
import com.ccomet.ailock.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AILockOverlayController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentView: View? = null

    fun show(context: Context, config: LockedAppConfig, timeLimitExceeded: Boolean) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) {
            appContext.startActivity(
                InterventionActivity.intent(appContext, config.packageName, timeLimitExceeded)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        val windowManager = appContext.getSystemService(WindowManager::class.java)
        dismiss(appContext)
        val view = buildOverlayView(appContext, config, timeLimitExceeded)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        runCatching {
            currentView = view
            windowManager.addView(view, params)
        }.onFailure {
            currentView = null
            appContext.startActivity(
                InterventionActivity.intent(appContext, config.packageName, timeLimitExceeded)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun dismiss(context: Context) {
        val view = currentView ?: return
        runCatching {
            context.getSystemService(WindowManager::class.java).removeView(view)
        }
        currentView = null
    }

    private fun buildOverlayView(
        context: Context,
        config: LockedAppConfig,
        timeLimitExceeded: Boolean,
    ): View {
        val density = context.resources.displayMetrics.density
        fun Int.dp(): Int = (this * density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(170, 45, 33, 24))
            setPadding(22.dp(), 22.dp(), 22.dp(), 22.dp())
        }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dp(), 22.dp(), 22.dp(), 22.dp())
            setBackgroundColor(Color.rgb(255, 251, 243))
        }
        root.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        card.addView(
            ImageView(context).apply {
                setImageResource(R.drawable.red_panda)
                adjustViewBounds = true
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                96.dp(),
            ),
        )

        val titleView = TextView(context).apply {
            text = if (timeLimitExceeded) {
                "잠깐! 약속 시간이 끝났어"
            } else {
                "잠깐! ${config.appName} 왜 켰어?"
            }
            textSize = 22f
            setTextColor(Color.rgb(45, 33, 24))
            setPadding(0, 12.dp(), 0, 8.dp())
        }
        val bodyView = TextView(context).apply {
            text = if (timeLimitExceeded) {
                "너 1분 넘게 더 쓰고 있어. 계속 써야 하는 이유가 있으면 나한테 말해줘."
            } else {
                "지금 이 앱을 꼭 써야 해? 괜찮은지 같이 생각해볼게."
            }
            textSize = 15f
            setTextColor(Color.rgb(89, 47, 24))
            setPadding(0, 0, 0, 12.dp())
        }
        val reasonInput = EditText(context).apply {
            hint = "왜 이 앱을 켜려고 하는지 적어줘."
            minLines = 3
            gravity = Gravity.TOP
        }
        val progress = ProgressBar(context).apply { visibility = View.GONE }
        val primary = Button(context).apply { text = "레서판다에게 물어보기" }
        val stop = Button(context).apply { text = "이번엔 참아볼게" }

        card.addView(titleView)
        card.addView(bodyView)
        card.addView(reasonInput)
        card.addView(progress)
        card.addView(primary)
        card.addView(stop)

        stop.setOnClickListener {
            scope.launch {
                val repository = AILockContainer.get(context).ailockRepository
                repository.recordEvent(config.packageName, config.appName, UsageEventType.SELF_STOP)
                repository.adjustWillPower(+3)
                dismiss(context)
                goHome(context)
            }
        }

        primary.setOnClickListener {
            val reason = reasonInput.text.toString().trim()
            if (reason.isBlank()) {
                bodyView.text = "이유를 한 줄만 적어줘. 나도 네 편이 되고 싶어."
                return@setOnClickListener
            }
            titleView.text = "흠... 잠깐만"
            bodyView.text = "이 이유가 괜찮은지 생각해볼게."
            reasonInput.visibility = View.GONE
            progress.visibility = View.VISIBLE
            primary.isEnabled = false
            stop.isEnabled = false

            scope.launch {
                val response = requestDecision(context, config, reason)
                progress.visibility = View.GONE
                stop.isEnabled = true
                primary.isEnabled = true
                titleView.text = when (response.status) {
                    AiDecisionStatus.OPTIMAL -> "좋아, 목적이 분명해"
                    AiDecisionStatus.WARNING -> "딱 약속한 만큼만이야"
                    AiDecisionStatus.OVERUSE -> "너 지금 조금 위험해"
                    AiDecisionStatus.CRITICAL -> "지금은 멈추자"
                    AiDecisionStatus.FAIL -> "판단이 애매해"
                }
                bodyView.text = "${response.text}\n${sourceLabel(response.source)} · 허용 시간: ${response.allowedTime}분"
                primary.text = when {
                    response.source == "network-error" -> "설정 확인할게"
                    response.allowedTime > 0 && response.status != AiDecisionStatus.CRITICAL -> "${config.appName} 사용하기"
                    else -> "그래도 사용할래"
                }
                primary.setOnClickListener {
                    scope.launch { applyDecision(context, config, response) }
                }
            }
        }

        return root
    }

    private suspend fun requestDecision(
        context: Context,
        config: LockedAppConfig,
        userReason: String,
    ): AiDecisionResponse {
        val container = AILockContainer.get(context)
        val records = container.ailockRepository.usageRecords.first()
        val today = TimeUtils.todayRecords(records).filter { it.packageName == config.packageName }
        val willPower = container.ailockRepository.willPowerScore.first()
        val request = AiDecisionRequest(
            appName = config.appName,
            userInput = userReason,
            lockReason = config.lockReasonFinal,
            currentStats = CurrentStats(
                willPowerScore = willPower,
                todayOpenAppCount = today.count { it.eventType == UsageEventType.OPEN || it.eventType == UsageEventType.AI_REQUEST },
                accumUseApp = today.sumOf { it.durationMinutes },
            ),
        )
        container.ailockRepository.recordEvent(
            packageName = config.packageName,
            appName = config.appName,
            eventType = UsageEventType.AI_REQUEST,
            userInput = userReason,
            lockReason = config.lockReasonFinal,
        )
        container.ailockRepository.adjustWillPower(-2)
        return container.aiDecisionRepository.decide(
            request = request,
            baseUrl = container.ailockRepository.backendBaseUrl.first(),
            useMock = container.ailockRepository.mockAiMode.first(),
        )
    }

    private suspend fun applyDecision(
        context: Context,
        config: LockedAppConfig,
        response: AiDecisionResponse,
    ) {
        val repository = AILockContainer.get(context).ailockRepository
        if (response.source == "network-error") {
            dismiss(context)
            return
        }
        if (response.allowedTime > 0 && response.status != AiDecisionStatus.CRITICAL) {
            repository.grantTemporaryAllowance(config.packageName, response.allowedTime)
            repository.recordEvent(
                packageName = config.packageName,
                appName = config.appName,
                eventType = UsageEventType.AI_ALLOWED,
                aiStatus = response.status.name,
                aiAllowedTime = response.allowedTime,
                lockReason = config.lockReasonFinal,
            )
            dismiss(context)
            AILockContainer.get(context).notificationHelper.notify(
                1001,
                "${config.appName} ${response.allowedTime}분 허용",
                "끝나면 레서판다가 다시 마무리를 도와줄게요.",
            )
            scope.launch {
                delay(response.allowedTime * 60_000L)
                OverlayService.show(context, config.packageName, timeLimitExceeded = true)
            }
        } else {
            repository.recordEvent(
                packageName = config.packageName,
                appName = config.appName,
                eventType = UsageEventType.AI_DENIED,
                aiStatus = response.status.name,
                aiAllowedTime = response.allowedTime,
                lockReason = config.lockReasonFinal,
            )
            repository.adjustWillPower(-5)
            dismiss(context)
            context.startActivity(
                InterventionActivity.intent(context, config.packageName, timeLimitExceeded = true, pledgeMode = true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun goHome(context: Context) {
        if (AILockAccessibilityService.goHomeIfAvailable()) return
        context.startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun sourceLabel(source: String?): String = when (source) {
        "server" -> "실제 서버 응답"
        "mock" -> "Mock 판단"
        "network-error" -> "서버 연결 실패"
        else -> "판단 결과"
    }
}
