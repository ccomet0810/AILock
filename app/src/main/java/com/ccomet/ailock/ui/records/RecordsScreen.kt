package com.ccomet.ailock.ui.records

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.UsageEventType
import com.ccomet.ailock.data.model.UsageRecord
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaOrange
import com.ccomet.ailock.ui.theme.AILockShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecordsScreen(uiState: AILockUiState) {
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf(HistoryRange.Day) }
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
    var bars by remember { mutableStateOf<List<UsageBar>>(emptyList()) }
    var appUsage by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    val earliestRecordDate = remember(uiState.usageRecords) {
        uiState.usageRecords.minOfOrNull {
            Instant.ofEpochMilli(it.openedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: LocalDate.now()
    }

    val summary = remember(uiState.usageRecords, selectedRange, anchorDate, appUsage, bars) {
        buildRecordSummary(
            records = uiState.usageRecords,
            range = selectedRange,
            anchorDate = anchorDate,
            totalMillis = bars.sumOf { it.totalTimeMillis },
            topAppName = appUsage.firstOrNull()?.appName ?: "없음",
        )
    }

    LaunchedEffect(uiState.permissions.hasUsageAccess, selectedRange, anchorDate) {
        if (uiState.permissions.hasUsageAccess) {
            val snapshot = withContext(Dispatchers.IO) {
                loadUsageSnapshot(context, selectedRange, anchorDate)
            }
            bars = snapshot.bars
            appUsage = snapshot.apps
        } else {
            bars = emptyList()
            appUsage = emptyList()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "기록",
                        modifier = Modifier.padding(start = 4.dp),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    RangeSwitcher(
                        selectedRange = selectedRange,
                        onRangeSelected = {
                            selectedRange = it
                            anchorDate = anchorDate.coerceAtMost(LocalDate.now())
                        },
                    )
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            if (!uiState.permissions.hasUsageAccess) {
                item {
                    EmptyStateCard(
                        title = "사용 기록 접근이 필요해요",
                        body = "앱 사용량과 시간대별 기록을 보려면 사용 기록 접근을 허용해 주세요.",
                    )
                }
            } else {
                stickyHeader {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 2.dp, bottom = 10.dp),
                    ) {
                        PeriodNavigator(
                            selectedRange = selectedRange,
                            anchorDate = anchorDate,
                            earliestDate = earliestRecordDate,
                            onMove = { delta ->
                                anchorDate = when (selectedRange) {
                                    HistoryRange.Day -> anchorDate.plusDays(delta.toLong())
                                    HistoryRange.Week -> anchorDate.plusDays((delta * 7).toLong())
                                }.coerceInDates(earliestRecordDate, LocalDate.now())
                            },
                        )
                    }
                }
                item {
                    SectionHeader(title = "한눈에 보기")
                    SummaryCarousel(summary)
                }
                item {
                    SectionHeader(title = "그래프")
                    UsageBarGraph(bars = bars)
                }
                item {
                    SectionHeader(title = "앱별 사용 시간")
                    AppUsageList(items = appUsage)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTextStrong,
        )
        trailing?.invoke()
    }
}

@Composable
private fun RangeSwitcher(
    selectedRange: HistoryRange,
    onRangeSelected: (HistoryRange) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HistoryRange.entries.forEach { range ->
            val selected = range == selectedRange
            Text(
                text = range.label,
                modifier = Modifier.clickable { onRangeSelected(range) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) AppTextStrong else AppTextSubtle.copy(alpha = 0.58f),
            )
        }
    }
}

@Composable
private fun PeriodNavigator(
    selectedRange: HistoryRange,
    anchorDate: LocalDate,
    earliestDate: LocalDate,
    onMove: (Int) -> Unit,
) {
    val previousAnchor = when (selectedRange) {
        HistoryRange.Day -> anchorDate.minusDays(1)
        HistoryRange.Week -> anchorDate.minusDays(7)
    }
    val canMovePrevious = !previousAnchor.isBefore(earliestDate)
    val canMoveNext = anchorDate < LocalDate.now()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = { onMove(-1) },
            enabled = canMovePrevious,
            modifier = Modifier.width(40.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "이전",
                tint = if (canMovePrevious) AppTextStrong else AppTextSubtle.copy(alpha = 0.35f),
            )
        }
        Text(
            text = selectedRange.periodLabel(anchorDate),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTextStrong,
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = { onMove(1) },
            enabled = canMoveNext,
            modifier = Modifier.width(40.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "다음",
                tint = if (canMoveNext) AppTextStrong else AppTextSubtle.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun SummaryCarousel(summary: RecordSummary) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        item {
            SummaryMetricCard(
                label = "총 사용 시간",
                value = formatUsageTimeStacked(summary.totalMillis),
            )
        }
        item {
            SummaryMetricCard(
                label = "가장 많이\n사용한 앱",
                value = summary.topAppName,
            )
        }
        item {
            SummaryMetricCard(
                label = "총 해제\n요청 횟수",
                value = "${summary.requestCount}회",
            )
        }
        item {
            SummaryMetricCard(
                label = "요청 해제\n성공률",
                value = "${summary.successRatePercent}%",
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .width(164.dp)
            .height(126.dp)
            .border(1.dp, AppBorder, AILockShape.card)
            .background(AppSurface, AILockShape.card)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppTextSubtle,
            lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppTextStrong,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 0.92f,
        )
    }
}

@Composable
private fun UsageBarGraph(bars: List<UsageBar>) {
    val actualMaxUsage = bars.maxOfOrNull { it.totalTimeMillis }?.coerceAtLeast(1L) ?: 1L
    val axisMaxUsage = niceGraphAxisMaxMillis(actualMaxUsage)
    val isHourly = bars.size == 24

    RecordCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        if (bars.isEmpty()) {
            Text(
                text = "표시할 사용량이 없어요",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSubtle,
            )
        } else {
            UsageGraphCanvas(
                bars = bars,
                axisMaxUsage = axisMaxUsage,
                isHourly = isHourly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

@Composable
private fun UsageGraphCanvas(
    bars: List<UsageBar>,
    axisMaxUsage: Long,
    isHourly: Boolean,
    modifier: Modifier = Modifier,
) {
    val axisColor = AppBorder
    val labelColor = AppTextSubtle
    val emptyColor = AppSurfaceMuted
    val barColor = PandaOrange
    val topLabel = compactGraphAxisLabel(axisMaxUsage)
    val middleLabel = compactGraphAxisLabel(axisMaxUsage / 2)
    val bottomLabels = if (isHourly) {
        listOf("00" to 0, "06" to 6, "12" to 12, "18" to 18, "24" to 24)
    } else {
        bars.mapIndexed { index, bar ->
            bar.label to index
        }
    }

    Canvas(modifier = modifier) {
        val left = 10.dp.toPx()
        val rightLabelWidth = 38.dp.toPx()
        val right = size.width - rightLabelWidth
        val top = 16.dp.toPx()
        val bottom = size.height - 34.dp.toPx()
        val middle = (top + bottom) / 2f
        val stroke = 1.dp.toPx()

        listOf(top, middle, bottom).forEach { y ->
            drawLine(
                color = axisColor,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = stroke,
            )
        }

        val graphHeight = bottom - top
        val slotWidth = (right - left) / bars.size.coerceAtLeast(1)

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb()
                textSize = 11.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
            }
            drawText(topLabel, right + 9.dp.toPx(), top + 5.dp.toPx(), paint)
            drawText(middleLabel, right + 9.dp.toPx(), middle + 5.dp.toPx(), paint)

            val bottomPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb()
                textSize = 12.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            bottomLabels.forEach { (label, index) ->
                val x = if (isHourly && index == 24) {
                    right
                } else {
                    left + slotWidth * index.coerceAtMost((bars.size - 1).coerceAtLeast(0)) + slotWidth / 2f
                }
                drawText(label, x, size.height - 5.dp.toPx(), bottomPaint)
            }
        }

        val barWidth = if (isHourly) 7.dp.toPx() else slotWidth.coerceAtMost(16.dp.toPx())
        bars.forEachIndexed { index, bar ->
            val ratio = (bar.totalTimeMillis.toFloat() / axisMaxUsage.toFloat()).coerceIn(0f, 1f)
            val barHeight = if (bar.totalTimeMillis > 0L) {
                (graphHeight * ratio).coerceAtLeast(barWidth + 4.dp.toPx())
            } else {
                0f
            }
            val centerX = left + slotWidth * index + slotWidth / 2f
            val barLeft = centerX - barWidth / 2f
            val barTop = bottom - barHeight
            val color = if (bar.totalTimeMillis > 0L) barColor else emptyColor

            if (barHeight > 0f) {
                val capRadius = barWidth / 2f
                drawRect(
                    color = color,
                    topLeft = Offset(barLeft, barTop + capRadius),
                    size = Size(barWidth, (barHeight - capRadius).coerceAtLeast(1f)),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barWidth),
                    cornerRadius = CornerRadius(capRadius, capRadius),
                )
            }
        }
    }
}

@Composable
private fun AppUsageList(items: List<AppUsage>) {
    val maxUsage = items.maxOfOrNull { it.totalTimeMillis }?.coerceAtLeast(1L) ?: 1L

    if (items.isEmpty()) {
        RecordCard {
            Text(
                text = "표시할 앱 사용량이 없어요",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSubtle,
            )
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppBorder),
        ) {
            Column {
                items.take(8).forEachIndexed { index, item ->
                    AppUsageRow(item = item, progress = item.totalTimeMillis.toFloat() / maxUsage)
                    if (index != items.take(8).lastIndex) {
                        HorizontalDivider(color = AppBorder)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(item: AppUsage, progress: Float) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatUsageTime(item.totalTimeMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTextSubtle,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppSurfaceMuted),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PandaOrange),
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String) {
    RecordCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTextStrong,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTextSubtle,
        )
    }
}

@Composable
private fun RecordCard(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private fun loadUsageSnapshot(context: Context, range: HistoryRange, anchorDate: LocalDate): UsageSnapshot {
    val manager = context.getSystemService(UsageStatsManager::class.java)
    val packageManager = context.packageManager
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val safeAnchor = anchorDate.coerceAtMost(today)
    val startDate = when (range) {
        HistoryRange.Day -> safeAnchor
        HistoryRange.Week -> safeAnchor.minusDays(6)
    }
    val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = if (safeAnchor == today) {
        System.currentTimeMillis()
    } else {
        safeAnchor.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val bars = when (range) {
        HistoryRange.Day -> hourlyUsage(manager, zone, startDate, end)
        HistoryRange.Week -> dayUsage(manager, zone, startDate, safeAnchor, end)
    }
    val apps = appUsageForPeriod(packageManager, manager, start, end)
    return UsageSnapshot(bars = bars, apps = apps)
}

private fun hourlyUsage(manager: UsageStatsManager, zone: ZoneId, date: LocalDate, endMillis: Long): List<UsageBar> {
    val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEndMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val totals = LongArray(24)
    val events = manager.queryEvents(startMillis, minOf(endMillis, dayEndMillis))
    val event = UsageEvents.Event()
    var activePackage: String? = null
    var activeStart = 0L

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val packageName = event.packageName ?: continue
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED,
            usageMoveToForegroundEvent() -> {
                activePackage?.let { addHourlyUsage(totals, activeStart, event.timeStamp, zone) }
                activePackage = packageName
                activeStart = event.timeStamp
            }

            UsageEvents.Event.ACTIVITY_PAUSED,
            usageMoveToBackgroundEvent() -> {
                if (activePackage == packageName) {
                    addHourlyUsage(totals, activeStart, event.timeStamp, zone)
                    activePackage = null
                    activeStart = 0L
                }
            }
        }
    }
    activePackage?.let {
        addHourlyUsage(totals, activeStart, minOf(System.currentTimeMillis(), dayEndMillis), zone)
    }

    return totals.mapIndexed { hour, total -> UsageBar("${hour}시", total) }
}

@Suppress("DEPRECATION")
private fun usageMoveToForegroundEvent(): Int = UsageEvents.Event.MOVE_TO_FOREGROUND

@Suppress("DEPRECATION")
private fun usageMoveToBackgroundEvent(): Int = UsageEvents.Event.MOVE_TO_BACKGROUND

private fun addHourlyUsage(totals: LongArray, startMillis: Long, endMillis: Long, zone: ZoneId) {
    if (endMillis <= startMillis) return

    var cursor = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDateTime()
    val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDateTime()
    while (cursor.isBefore(end)) {
        val nextHour = cursor.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        val segmentEnd = minOf(nextHour, end)
        val hour = cursor.hour
        if (hour in totals.indices) {
            totals[hour] += ChronoUnit.MILLIS.between(cursor, segmentEnd)
        }
        cursor = segmentEnd
    }
}

private fun dayUsage(
    manager: UsageStatsManager,
    zone: ZoneId,
    startDate: LocalDate,
    endDate: LocalDate,
    endMillis: Long,
): List<UsageBar> =
    (0..6).map { offset ->
        val date = startDate.plusDays(offset.toLong())
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = if (date == endDate) {
            endMillis
        } else {
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        UsageBar(
            label = "${date.monthValue}/${date.dayOfMonth}",
            totalTimeMillis = manager.queryAndAggregateUsageStats(start, end).values.sumOf { it.totalTimeInForeground },
        )
    }

private fun appUsageForPeriod(
    packageManager: PackageManager,
    manager: UsageStatsManager,
    startMillis: Long,
    endMillis: Long,
): List<AppUsage> =
    manager.queryAndAggregateUsageStats(startMillis, endMillis)
        .values
        .asSequence()
        .filter { it.totalTimeInForeground > 0L }
        .map { stats ->
            AppUsage(
                packageName = stats.packageName,
                appName = packageManager.safeLabel(stats.packageName),
                icon = packageManager.safeIcon(stats.packageName),
                totalTimeMillis = stats.totalTimeInForeground,
            )
        }
        .sortedByDescending { it.totalTimeMillis }
        .toList()

private fun buildRecordSummary(
    records: List<UsageRecord>,
    range: HistoryRange,
    anchorDate: LocalDate,
    totalMillis: Long,
    topAppName: String,
): RecordSummary {
    val zone = ZoneId.systemDefault()
    val startDate = if (range == HistoryRange.Day) anchorDate else anchorDate.minusDays(6)
    val scoped = records.filter {
        val date = Instant.ofEpochMilli(it.openedAt).atZone(zone).toLocalDate()
        !date.isBefore(startDate) && !date.isAfter(anchorDate)
    }
    val requestCount = scoped.count { it.eventType == UsageEventType.AI_REQUEST }
    val successCount = scoped.count { it.eventType == UsageEventType.AI_ALLOWED }
    val successRate = if (requestCount == 0) 0 else ((successCount * 100f) / requestCount).toInt()

    return RecordSummary(
        totalMillis = totalMillis,
        requestCount = requestCount,
        topAppName = topAppName,
        successRatePercent = successRate,
    )
}

private fun PackageManager.safeLabel(packageName: String): String =
    runCatching { getApplicationLabel(getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName)

private fun PackageManager.safeIcon(packageName: String): Drawable? =
    runCatching { getApplicationIcon(packageName) }.getOrNull()

private fun formatUsageTime(totalTimeMillis: Long): String {
    val totalSeconds = (totalTimeMillis / 1_000L).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "%02d시간 %02d분 %02d초".format(hours, minutes, seconds)
}

private fun formatUsageTimeStacked(totalTimeMillis: Long): String {
    val totalSeconds = (totalTimeMillis / 1_000L).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "%02d시간\n%02d분 %02d초".format(hours, minutes, seconds)
}

private fun compactUsageTime(totalTimeMillis: Long): String {
    val minutes = totalTimeMillis / 60_000L
    val hours = minutes / 60
    val remain = minutes % 60
    return when {
        hours > 0 -> "${hours}h"
        remain > 0 -> "${remain}m"
        else -> ""
    }
}

private fun niceGraphAxisMaxMillis(actualMaxMillis: Long): Long {
    val actualMinutes = ((actualMaxMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
    val axisMinutes = if (actualMinutes <= 60L) {
        listOf(2L, 4L, 6L, 10L, 20L, 30L, 60L).firstOrNull { actualMinutes <= it } ?: 60L
    } else {
        val actualHours = ((actualMinutes + 59L) / 60L).coerceAtLeast(1L)
        val evenHours = if (actualHours % 2L == 0L) actualHours else actualHours + 1L
        evenHours * 60L
    }
    return axisMinutes * 60_000L
}

private fun compactGraphAxisLabel(totalTimeMillis: Long): String {
    val minutes = (totalTimeMillis / 60_000L).coerceAtLeast(0)
    return if (minutes >= 60L) "${minutes / 60L}시간" else "${minutes}분"
}

private fun LocalDate.coerceAtMost(maximum: LocalDate): LocalDate =
    if (isAfter(maximum)) maximum else this

private fun LocalDate.coerceInDates(minimum: LocalDate, maximum: LocalDate): LocalDate =
    when {
        isBefore(minimum) -> minimum
        isAfter(maximum) -> maximum
        else -> this
    }

private enum class HistoryRange(val label: String) {
    Day("1일"),
    Week("7일");

    fun periodLabel(anchorDate: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("M월 d일")
        return when (this) {
            Day -> anchorDate.format(formatter)
            Week -> "${anchorDate.minusDays(6).format(formatter)} - ${anchorDate.format(formatter)}"
        }
    }
}

private data class UsageSnapshot(val bars: List<UsageBar>, val apps: List<AppUsage>)

private data class RecordSummary(
    val totalMillis: Long,
    val requestCount: Int,
    val topAppName: String,
    val successRatePercent: Int,
)

private data class UsageBar(val label: String, val totalTimeMillis: Long)

private data class AppUsage(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val totalTimeMillis: Long,
)


