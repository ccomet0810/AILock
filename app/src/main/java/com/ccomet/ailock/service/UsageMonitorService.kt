package com.ccomet.ailock.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ccomet.ailock.AILockContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsageMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val helper = AILockContainer.get(applicationContext).notificationHelper
        startForeground(NotificationHelper.FOREGROUND_ID, helper.foregroundNotification())
        scope.launch {
            while (isActive) {
                delay(60_000L)
                helper.notify(
                    1002,
                    "AILock이 실행 중이에요",
                    "제한 앱을 오래 쓰면 레서판다가 먼저 말을 걸 거예요.",
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
