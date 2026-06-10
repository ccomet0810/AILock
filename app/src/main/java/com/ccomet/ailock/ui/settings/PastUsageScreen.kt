package com.ccomet.ailock.ui.settings

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val PAST_USAGE_LOOKBACK_DAYS = 30L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastUsageScreen(
    uiState: AILockUiState,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = remember { LocalDate.now() }
    val earliestDate = remember { today.minusDays(PAST_USAGE_LOOKBACK_DAYS) }
    var selectedDate by remember { mutableStateOf(today) }
    var snapshot by remember { mutableStateOf(PastUsageSnapshot.EMPTY) }

    LaunchedEffect(uiState.permissions.hasUsageAccess, selectedDate) {
        snapshot = if (uiState.permissions.hasUsageAccess) {
            withContext(Dispatchers.IO) {
                loadPastUsageSnapshot(context, selectedDate)
            }
        } else {
            PastUsageSnapshot.EMPTY
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("과거 사용 기록", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = AILockSpacing.screenHorizontal,
                top = AILockSpacing.sectionGap,
                end = AILockSpacing.screenHorizontal,
                bottom = AILockLayout.scrollContentBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap),
        ) {
            item {
                DateNavigator(
                    selectedDate = selectedDate,
                    earliestDate = earliestDate,
                    today = today,
                    onMove = { days ->
                        selectedDate = selectedDate.plusDays(days).coerceInDates(earliestDate, today)
                    },
                )
            }
            if (!uiState.permissions.hasUsageAccess) {
                item {
                    AilockCard(verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap)) {
                        Text("사용 기록 접근이 필요해요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Android 설정에서 AILock의 사용 기록 접근을 허용하면 OS에 남아 있는 과거 앱 사용 기록을 볼 수 있어요.", color = AppTextSubtle)
                    }
                }
            } else {
                item {
                    AilockCard(verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap)) {
                        Text("총 스크린타임", style = MaterialTheme.typography.labelLarge, color = AppTextSubtle)
                        Text(formatUsageTime(snapshot.totalMillis), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppTextStrong)
                        Text("앱 실행/종료 이벤트를 해당 날짜 안으로 잘라 계산한 값이에요.", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
                    }
                }
                item {
                    Text("앱별 사용 시간", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppTextStrong)
                }
                if (snapshot.apps.isEmpty()) {
                    item {
                        AilockCard {
                            Text("표시할 앱 사용량이 없어요", color = AppTextSubtle)
                        }
                    }
                } else {
                    item {
                        AilockCard(contentPadding = PaddingValues(0.dp)) {
                            Column {
                                snapshot.apps.take(20).forEachIndexed { index, item ->
                                    PastUsageRow(item = item, maxMillis = snapshot.apps.first().totalMillis)
                                    if (index != snapshot.apps.take(20).lastIndex) {
                                        HorizontalDivider(color = AppBorder)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavigator(
    selectedDate: LocalDate,
    earliestDate: LocalDate,
    today: LocalDate,
    onMove: (Long) -> Unit,
) {
    AilockCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onMove(-1) }, enabled = selectedDate > earliestDate) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "이전")
            }
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("M월 d일")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { onMove(1) }, enabled = selectedDate < today) {
                Icon(Icons.Default.ChevronRight, contentDescription = "다음")
            }
        }
    }
}

@Composable
private fun PastUsageRow(item: PastAppUsage, maxMillis: Long) {
    val progress = (item.totalMillis.toFloat() / maxMillis.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.padding(AILockSpacing.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
    ) {
        InstalledAppIcon(
            app = InstalledAppInfo(
                packageName = item.packageName,
                appName = item.appName,
                icon = item.icon,
            ),
            size = 48.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.appName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTextStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(AILockSpacing.buttonIconGap))
                Text(formatUsageTime(item.totalMillis), style = MaterialTheme.typography.labelMedium, color = AppTextSubtle)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .padding(end = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .align(Alignment.CenterStart),
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AILockShape.graphBar)
                            .background(AppSurfaceMuted),
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(10.dp)
                            .clip(AILockShape.graphBar)
                            .background(PandaOrange),
                    )
                }
            }
        }
    }
}

private fun loadPastUsageSnapshot(context: Context, date: LocalDate): PastUsageSnapshot {
    val manager = context.getSystemService(UsageStatsManager::class.java)
    val packageManager = context.packageManager
    val zone = ZoneId.systemDefault()
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val queryStart = date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val totals = mutableMapOf<String, Long>()
    val events = manager.queryEvents(queryStart, end)
    val event = UsageEvents.Event()
    var activePackage: String? = null
    var activeStart = queryStart

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val packageName = event.packageName ?: continue
        val eventTime = event.timeStamp.coerceIn(queryStart, end)
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED,
            usageMoveToForegroundEvent() -> {
                activePackage?.let { addClippedUsage(totals, it, activeStart, eventTime, start, end) }
                activePackage = packageName
                activeStart = eventTime
            }

            UsageEvents.Event.ACTIVITY_PAUSED,
            usageMoveToBackgroundEvent() -> {
                if (activePackage == packageName) {
                    addClippedUsage(totals, packageName, activeStart, eventTime, start, end)
                    activePackage = null
                    activeStart = eventTime
                }
            }
        }
    }
    activePackage?.let { addClippedUsage(totals, it, activeStart, end, start, end) }

    val periodDuration = end - start
    val apps = totals.asSequence()
        .filter { (_, millis) -> millis > 0L }
        .map { (packageName, millis) ->
            PastAppUsage(
                packageName = packageName,
                appName = packageManager.safeLabel(packageName),
                icon = packageManager.safeIcon(packageName),
                totalMillis = millis.coerceAtMost(periodDuration),
            )
        }
        .sortedByDescending { it.totalMillis }
        .toList()

    return PastUsageSnapshot(
        apps = apps,
        totalMillis = apps.sumOf { it.totalMillis }.coerceAtMost(periodDuration),
    )
}

private fun addClippedUsage(
    totals: MutableMap<String, Long>,
    packageName: String,
    rawStart: Long,
    rawEnd: Long,
    windowStart: Long,
    windowEnd: Long,
) {
    val start = maxOf(rawStart, windowStart)
    val end = minOf(rawEnd, windowEnd)
    if (end <= start) return
    totals[packageName] = (totals[packageName] ?: 0L) + (end - start)
}

@Suppress("DEPRECATION")
private fun usageMoveToForegroundEvent(): Int = UsageEvents.Event.MOVE_TO_FOREGROUND

@Suppress("DEPRECATION")
private fun usageMoveToBackgroundEvent(): Int = UsageEvents.Event.MOVE_TO_BACKGROUND

private fun PackageManager.safeLabel(packageName: String): String =
    runCatching { getApplicationLabel(getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName)

private fun PackageManager.safeIcon(packageName: String): Drawable? =
    runCatching { getApplicationIcon(packageName) }.getOrNull()

private fun formatUsageTime(totalMillis: Long): String {
    val totalMinutes = (totalMillis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
}

private fun LocalDate.coerceInDates(start: LocalDate, end: LocalDate): LocalDate =
    when {
        isBefore(start) -> start
        isAfter(end) -> end
        else -> this
    }

private data class PastUsageSnapshot(
    val apps: List<PastAppUsage>,
    val totalMillis: Long,
) {
    companion object {
        val EMPTY = PastUsageSnapshot(emptyList(), 0L)
    }
}

private data class PastAppUsage(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val totalMillis: Long,
)
