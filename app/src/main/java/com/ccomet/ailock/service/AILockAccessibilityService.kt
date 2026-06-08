package com.ccomet.ailock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.data.work.SessionWorkScheduler
import com.ccomet.ailock.domain.model.BlockDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AILockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastOverlayPackage: String? = null
    private var lastOverlayAt: Long = 0L
    private var lastRecordedOpenPackage: String? = null
    private val pendingExitJobs = mutableMapOf<String, Job>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
        Log.d(TAG, "AILock accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        if (packageName.isBlank()) return
        lastObservedForegroundPackage = packageName
        dismissOverlayIfHomeOpened(packageName)

        scope.launch {
            val container = AILockContainer.get(applicationContext)
            handleSessionExitDebounce(container, packageName)
            val lockedApps = container.ailockRepository.lockedApps.first()
            val isTrackedLockedApp = lockedApps.any { it.packageName == packageName }
            if (!isTrackedLockedApp) {
                lastRecordedOpenPackage = null
            } else if (lastRecordedOpenPackage != packageName) {
                container.blockingEngine.recordOpen(packageName)
                lastRecordedOpenPackage = packageName
            }
            when (val decision = container.blockingEngine.evaluate(packageName)) {
                BlockDecision.Allow -> Unit
                is BlockDecision.ShowIntervention -> showOverlayWithCooldown(decision)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AILock accessibility service interrupted")
    }

    override fun onDestroy() {
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    private suspend fun showOverlayWithCooldown(decision: BlockDecision.ShowIntervention) {
        val now = System.currentTimeMillis()
        if (AILockOverlayController.isShowingFor(decision.config.packageName)) {
            return
        }
        if (lastOverlayPackage == decision.config.packageName && now - lastOverlayAt < OVERLAY_COOLDOWN_MS) {
            return
        }
        lastOverlayPackage = decision.config.packageName
        lastOverlayAt = now
        val container = AILockContainer.get(applicationContext)
        AILockOverlayController.show(
            context = applicationContext,
            config = decision.config,
            timeLimitExceeded = decision.timeLimitExceeded,
            initialSession = container.activeUseSessionRepository.get(decision.config.packageName),
            initialPending = container.pendingFinalDecisionRepository.get(decision.config.packageName),
        )
    }

    private fun dismissOverlayIfHomeOpened(packageName: String) {
        val showingPackage = AILockOverlayController.showingPackageName() ?: return
        if (showingPackage == packageName) return
        if (packageName in homePackages()) {
            AILockOverlayController.dismiss(applicationContext)
            resetOverlayCooldown()
        }
    }

    private fun homePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    private suspend fun handleSessionExitDebounce(container: AILockContainer, foregroundPackage: String) {
        val sessions = container.activeUseSessionRepository.getAll()
        sessions.forEach { (packageName, session) ->
            if (packageName == foregroundPackage) {
                pendingExitJobs.remove(packageName)?.cancel()
                SessionWorkScheduler.cancelForegroundExitUpdate(applicationContext, packageName)
            } else if (session.state != SESSION_STATE_IN_USE || AILockOverlayController.isShowingFor(packageName)) {
                pendingExitJobs.remove(packageName)?.cancel()
                SessionWorkScheduler.cancelForegroundExitUpdate(applicationContext, packageName)
            } else {
                if (pendingExitJobs[packageName]?.isActive != true) {
                    pendingExitJobs[packageName] = scope.launch {
                        delay(SESSION_EXIT_DEBOUNCE_MS)
                        finalizeSessionIfStillAway(packageName)
                    }
                }
                SessionWorkScheduler.scheduleForegroundExitUpdate(
                    context = applicationContext,
                    packageName = packageName,
                    delayMs = SESSION_EXIT_DEBOUNCE_MS,
                )
            }
        }
    }

    private suspend fun finalizeSessionIfStillAway(packageName: String) {
        val container = AILockContainer.get(applicationContext)
        val latest = container.activeUseSessionRepository.get(packageName) ?: return

        if (lastObservedForegroundPackage == packageName) return
        if (latest.state != SESSION_STATE_IN_USE) return
        if (AILockOverlayController.isShowingFor(packageName)) return

        if (latest.sessionId.isNotBlank()) {
            val totalMinutes = ((System.currentTimeMillis() - latest.startedAt) / 60_000L)
                .toInt()
                .coerceAtLeast(0)
            SessionWorkScheduler.enqueueSessionUpdate(applicationContext, latest.sessionId, totalMinutes)
        }

        container.pendingFinalDecisionRepository.clear(packageName)
        container.activeUseSessionRepository.clear(packageName)
        SessionWorkScheduler.cancelAllForPackage(applicationContext, packageName)
        pendingExitJobs.remove(packageName)
    }

    companion object {
        private const val TAG = "AILockAccessibility"
        private const val OVERLAY_COOLDOWN_MS = 8_000L
        private const val SESSION_EXIT_DEBOUNCE_MS = 60_000L
        private const val SESSION_STATE_IN_USE = "IN_USE"
        private var activeService: AILockAccessibilityService? = null
        private var lastObservedForegroundPackage: String? = null

        fun goHomeIfAvailable(): Boolean =
            activeService?.performGlobalAction(GLOBAL_ACTION_HOME) == true

        fun currentForegroundPackage(): String? = lastObservedForegroundPackage

        fun resetOverlayCooldown() {
            activeService?.lastOverlayPackage = null
            activeService?.lastOverlayAt = 0L
        }
    }
}

