package com.ccomet.ailock.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ccomet.ailock.AILockContainer
import com.ccomet.ailock.domain.model.BlockDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AILockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastOverlayPackage: String? = null
    private var lastOverlayAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
        Log.d(TAG, "AILock accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        if (packageName.isBlank()) return

        scope.launch {
            val container = AILockContainer.get(applicationContext)
            container.blockingEngine.recordOpen(packageName)
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

    private fun showOverlayWithCooldown(decision: BlockDecision.ShowIntervention) {
        val now = System.currentTimeMillis()
        if (lastOverlayPackage == decision.config.packageName && now - lastOverlayAt < OVERLAY_COOLDOWN_MS) {
            return
        }
        lastOverlayPackage = decision.config.packageName
        lastOverlayAt = now
        OverlayService.show(applicationContext, decision.config.packageName, decision.timeLimitExceeded)
    }

    companion object {
        private const val TAG = "AILockAccessibility"
        private const val OVERLAY_COOLDOWN_MS = 2_500L
        private var activeService: AILockAccessibilityService? = null

        fun goHomeIfAvailable(): Boolean =
            activeService?.performGlobalAction(GLOBAL_ACTION_HOME) == true
    }
}
