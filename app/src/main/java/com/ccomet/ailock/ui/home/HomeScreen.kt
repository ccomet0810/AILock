package com.ccomet.ailock.ui.home

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: AILockUiState) {
    val context = LocalContext.current
    var todayScreenTimeMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.permissions.hasUsageAccess) {
        todayScreenTimeMillis = if (uiState.permissions.hasUsageAccess) {
            withContext(Dispatchers.IO) {
                loadTodayScreenTimeMillis(context.getSystemService(UsageStatsManager::class.java))
            }
        } else {
            null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap),
            ) {
                PandaSpeechBubble(
                    text = homeMessage(uiState.lockedApps.size),
                    modifier = Modifier.fillMaxWidth(),
                )
                TodayScreenTimeCard(
                    hasUsageAccess = uiState.permissions.hasUsageAccess,
                    totalMillis = todayScreenTimeMillis,
                )
            }
        }
    }
}

@Composable
private fun TodayScreenTimeCard(hasUsageAccess: Boolean, totalMillis: Long?) {
    AilockCard(
        contentPadding = PaddingValues(AILockSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap),
    ) {
        Text(
            text = "오늘 총 스크린타임",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTextSubtle,
        )
        Text(
            text = when {
                !hasUsageAccess -> "권한 필요"
                totalMillis == null -> "불러오는 중"
                else -> formatScreenTime(totalMillis)
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppTextStrong,
        )
    }
}

private fun homeMessage(lockedAppCount: Int): String =
    if (lockedAppCount == 0) {
        "관리할 앱을 정하면 내가 옆에서 도와줄게."
    } else {
        "오늘도 약속한 만큼만 써보자."
    }

private fun loadTodayScreenTimeMillis(manager: UsageStatsManager): Long {
    val zone = ZoneId.systemDefault()
    val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = System.currentTimeMillis()
    val elapsedMillis = (end - start).coerceAtLeast(0L)
    val aggregateTotalMillis = manager.queryAndAggregateUsageStats(start, end).values.sumOf { it.totalTimeInForeground }
    val eventTotalMillis = eventTotalUsageForPeriod(manager, start, end)
    return when {
        aggregateTotalMillis in 1..elapsedMillis -> aggregateTotalMillis
        eventTotalMillis > 0L -> eventTotalMillis
        else -> 0L
    }.coerceAtMost(elapsedMillis)
}

private fun eventTotalUsageForPeriod(manager: UsageStatsManager, startMillis: Long, endMillis: Long): Long {
    val events = manager.queryEvents(startMillis, endMillis)
    val event = UsageEvents.Event()
    var totalMillis = 0L
    var activePackage: String? = null
    var activeStart = startMillis

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val packageName = event.packageName ?: continue
        val eventTime = event.timeStamp.coerceIn(startMillis, endMillis)
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED,
            usageMoveToForegroundEvent() -> {
                activePackage?.let { totalMillis += (eventTime - activeStart).coerceAtLeast(0L) }
                activePackage = packageName
                activeStart = eventTime
            }

            UsageEvents.Event.ACTIVITY_PAUSED,
            usageMoveToBackgroundEvent() -> {
                if (activePackage == packageName) {
                    totalMillis += (eventTime - activeStart).coerceAtLeast(0L)
                    activePackage = null
                    activeStart = eventTime
                }
            }
        }
    }
    if (activePackage != null) {
        totalMillis += (endMillis - activeStart).coerceAtLeast(0L)
    }
    return totalMillis.coerceAtMost((endMillis - startMillis).coerceAtLeast(0L))
}

@Suppress("DEPRECATION")
private fun usageMoveToForegroundEvent(): Int = UsageEvents.Event.MOVE_TO_FOREGROUND

@Suppress("DEPRECATION")
private fun usageMoveToBackgroundEvent(): Int = UsageEvents.Event.MOVE_TO_BACKGROUND

private fun formatScreenTime(totalMillis: Long): String {
    val totalMinutes = (totalMillis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}시간 ${minutes}분"
    } else {
        "${minutes}분"
    }
}
