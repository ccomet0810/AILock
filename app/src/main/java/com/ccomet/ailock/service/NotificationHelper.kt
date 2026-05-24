package com.ccomet.ailock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ccomet.ailock.MainActivity
import com.ccomet.ailock.R

class NotificationHelper(private val context: Context) {
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun foregroundNotification(): android.app.Notification {
        ensureChannels()
        return baseBuilder(
            title = "AILock이 약속을 지켜보는 중",
            text = "제한 앱 사용을 감지하면 레서판다가 먼저 물어볼게요.",
        )
            .setOngoing(true)
            .build()
    }

    fun notify(id: Int, title: String, text: String) {
        ensureChannels()
        runCatching {
            NotificationManagerCompat.from(context).notify(
                id,
                baseBuilder(title, text).build(),
            )
        }
    }

    private fun baseBuilder(title: String, text: String): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    companion object {
        const val CHANNEL_ID = "ailock_promises"
        const val FOREGROUND_ID = 514
    }
}
