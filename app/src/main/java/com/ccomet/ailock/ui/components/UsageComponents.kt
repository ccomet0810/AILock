package com.ccomet.ailock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.PandaCream
import com.ccomet.ailock.ui.theme.PandaOrange
import com.ccomet.ailock.ui.theme.SoftGreenContainer
import com.ccomet.ailock.ui.theme.SoftRedContainer
import com.ccomet.ailock.util.TimeUtils
import kotlinx.coroutines.delay


@Composable
fun StatusPill(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(top = 6.dp),
        shape = AILockShape.pill,
        color = if (positive) SoftGreenContainer else SoftRedContainer,
        border = BorderStroke(1.dp, if (positive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AppUsageBar(label: String, minutes: Int, maxMinutes: Int, modifier: Modifier = Modifier) {
    val progress = if (maxMinutes <= 0) 0f else (minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(TimeUtils.minutesLabel(minutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(AILockShape.pill),
            color = PandaOrange,
            trackColor = PandaCream,
        )
    }
}

@Composable
fun LockedAppCard(
    config: LockedAppConfig,
    appInfo: InstalledAppInfo?,
    usedMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limitMinutes = config.dailyLimitMinutes ?: 120
    val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = AILockShape.card,
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier.padding(AILockSpacing.itemPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            if (appInfo != null) {
                InstalledAppIcon(appInfo, size = 48.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AILockShape.card)
                        .background(AppSurfaceMuted)
                        .border(1.dp, AppBorder, AILockShape.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(config.appName.take(1), fontWeight = FontWeight.Bold, color = AppTextStrong)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(config.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("총 사용시간 ${TimeUtils.minutesLabel(usedMinutes)}", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
            }
            StatusPill(
                text = "남은 ${TimeUtils.minutesLabel(remainingMinutes)}",
                positive = remainingMinutes > 0,
            )
        }
    }
}

@Composable
fun ActiveLockTimerList(
    configs: List<LockedAppConfig>,
    installedAppsByPackage: Map<String, InstalledAppInfo>,
    modifier: Modifier = Modifier,
    title: String = "작동 중인 잠금 타이머",
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val activeConfigs = configs
        .filter { (it.lockUntilAt ?: 0L) > now }
        .sortedBy { it.lockUntilAt ?: Long.MAX_VALUE }

    LaunchedEffect(activeConfigs.map { it.packageName to it.lockUntilAt }) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    if (activeConfigs.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppTextStrong)
        activeConfigs.forEach { config ->
            ActiveLockTimerCard(
                config = config,
                appInfo = installedAppsByPackage[config.packageName],
                now = now,
            )
        }
    }
}

@Composable
private fun ActiveLockTimerCard(
    config: LockedAppConfig,
    appInfo: InstalledAppInfo?,
    now: Long,
) {
    val remainingMs = ((config.lockUntilAt ?: 0L) - now).coerceAtLeast(0L)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AILockShape.card,
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier.padding(AILockSpacing.itemPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            if (appInfo != null) {
                InstalledAppIcon(appInfo, size = 48.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AILockShape.card)
                        .background(AppSurfaceMuted)
                        .border(1.dp, AppBorder, AILockShape.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(config.appName.take(1), fontWeight = FontWeight.Bold, color = AppTextStrong)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(config.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("잠금 해제까지 ${formatRemainingLockTime(remainingMs)}", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
            }
            StatusPill(text = "잠금 중", positive = false)
        }
    }
}

private fun formatRemainingLockTime(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%02d시간 %02d분 %02d초".format(hours, minutes, seconds)
    } else {
        "%02d분 %02d초".format(minutes, seconds)
    }
}

private fun timerLabel(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}"
}
