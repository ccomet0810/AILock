package com.ccomet.ailock.ui.records

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.AilockSegmentedControl
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.components.SectionTitle
import com.ccomet.ailock.ui.components.StickyCollapsingScreenHeader
import com.ccomet.ailock.ui.components.rememberAILockHeaderMotionState
import com.ccomet.ailock.ui.components.rememberAILockHeaderNestedScrollConnection
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaOrange
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val PeriodNavigatorHeight = AILockLayout.collapsedHeaderHeight
private const val OS_USAGE_LOOKBACK_DAYS = 30L

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecordsScreen(uiState: AILockUiState) {
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf(HistoryRange.Day) }
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
    var bars by remember { mutableStateOf<List<UsageBar>>(emptyList()) }
    var appUsage by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var usageTotalMillis by remember { mutableStateOf(0L) }
    var loadedSnapshotKey by remember { mutableStateOf<Pair<HistoryRange, LocalDate>?>(null) }
    val listState = rememberLazyListState()
    val headerMotion = rememberAILockHeaderMotionState(label = "recordsHeaderMotion")
    val headerNestedScrollConnection = rememberAILockHeaderNestedScrollConnection(headerMotion, listState)
    val earliestRecordDate = remember(uiState.usageRecords) {
        val osUsageEarliestDate = LocalDate.now().minusDays(OS_USAGE_LOOKBACK_DAYS)
        val ailockEarliestDate = uiState.usageRecords.minOfOrNull {
            Instant.ofEpochMilli(it.openedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        ailockEarliestDate?.coerceAtMost(osUsageEarliestDate) ?: osUsageEarliestDate
    }

    val displayedRange = loadedSnapshotKey?.first ?: selectedRange
    val displayedAnchorDate = loadedSnapshotKey?.second ?: anchorDate
    val hasDisplayedSnapshot = loadedSnapshotKey != null

    val summary = remember(uiState.usageRecords, displayedRange, displayedAnchorDate, appUsage, usageTotalMillis) {
        buildRecordSummary(
            records = uiState.usageRecords,
            range = displayedRange,
            anchorDate = displayedAnchorDate,
            totalMillis = usageTotalMillis,
            topAppName = appUsage.firstOrNull()?.appName ?: "없음",
        )
    }

    LaunchedEffect(uiState.permissions.hasUsageAccess, selectedRange, anchorDate) {
        if (uiState.permissions.hasUsageAccess) {
            val snapshotKey = selectedRange to anchorDate
            val snapshot = withContext(Dispatchers.IO) {
                loadUsageSnapshot(context, selectedRange, anchorDate)
            }
            bars = snapshot.bars
            appUsage = snapshot.apps
            usageTotalMillis = snapshot.totalMillis
            loadedSnapshotKey = snapshotKey
        } else {
            bars = emptyList()
            appUsage = emptyList()
            usageTotalMillis = 0L
            loadedSnapshotKey = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(headerNestedScrollConnection),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap),
                contentPadding = PaddingValues(bottom = AILockLayout.scrollContentBottomPadding),
            ) {
                item {
                    val fixedTopContentHeight = if (uiState.permissions.hasUsageAccess) {
                        PeriodNavigatorHeight
                    } else {
                        0.dp
                    }
                    Spacer(modifier = Modifier.height(headerMotion.currentHeaderHeight + fixedTopContentHeight))
                }
                if (!uiState.permissions.hasUsageAccess) {
                    item {
                        EmptyStateCard(
                            title = "사용 기록 접근이 필요해요",
                            body = "앱 사용량과 시간대별 기록을 보려면 사용 기록 접근을 허용해 주세요.",
                            modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
                        )
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal)) {
                            SummaryCarousel(summary)
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = AILockSpacing.screenHorizontal)
                                .padding(top = 6.dp),
                        ) {
                            SectionTitle(title = "그래프")
                            UsageBarGraph(
                                bars = bars,
                                selectedRange = displayedRange,
                                anchorDate = displayedAnchorDate,
                                isLoaded = hasDisplayedSnapshot,
                            )
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = AILockSpacing.screenHorizontal)
                                .padding(top = 6.dp),
                        ) {
                            SectionTitle(title = "앱별 사용 시간")
                            AppUsageList(items = appUsage)
                        }
                    }
                }
            }
            if (uiState.permissions.hasUsageAccess) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = headerMotion.currentHeaderHeight)
                        .height(PeriodNavigatorHeight)
                        .background(MaterialTheme.colorScheme.background),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AILockSpacing.screenHorizontal),
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = AILockSpacing.screenHorizontal),
                        color = AppBorder.copy(alpha = headerMotion.collapseFraction),
                    )
                }
            }
            StickyCollapsingScreenHeader(
                title = "기록",
                subtitle = "1일, 7일 선택하여 볼 수 있어요",
                collapseFraction = headerMotion.collapseFraction,
                modifier = Modifier.align(Alignment.TopCenter),
                actions = {
                    AilockSegmentedControl(
                        options = HistoryRange.entries,
                        selectedOption = selectedRange,
                        onOptionSelected = {
                            selectedRange = it
                            anchorDate = anchorDate.coerceAtMost(LocalDate.now())
                        },
                        label = { it.label },
                    )
                },
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
    modifier: Modifier = Modifier,
) {
    val previousAnchor = when (selectedRange) {
        HistoryRange.Day -> anchorDate.minusDays(1)
        HistoryRange.Week -> anchorDate.minusDays(7)
    }
    val canMovePrevious = !previousAnchor.isBefore(earliestDate)
    val canMoveNext = anchorDate < LocalDate.now()
    Row(
        modifier = modifier.fillMaxWidth(),
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
        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
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
private fun UsageBarGraph(
    bars: List<UsageBar>,
    selectedRange: HistoryRange,
    anchorDate: LocalDate,
    isLoaded: Boolean,
) {
    val graphBars = remember(bars, selectedRange, anchorDate) {
        bars.ifEmpty { selectedRange.emptyUsageBars(anchorDate) }
    }
    val actualMaxUsage = graphBars.maxOfOrNull { it.totalTimeMillis }?.coerceAtLeast(1L) ?: 1L
    val axisMaxUsage = niceGraphAxisMaxMillis(actualMaxUsage)
    val isHourly = selectedRange == HistoryRange.Day
    val animationKey = Triple(selectedRange, anchorDate, if (isLoaded) bars.sumOf { it.totalTimeMillis } else -1L)
    val revealProgress = remember { Animatable(0f) }
    LaunchedEffect(animationKey) {
        revealProgress.snapTo(0f)
        if (!isLoaded) return@LaunchedEffect
        withFrameNanos { }
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        )
    }

    RecordCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            UsageGraphCanvas(
                bars = graphBars,
                axisMaxUsage = axisMaxUsage,
                isHourly = isHourly,
                showAxisLabels = isLoaded,
                revealProgress = revealProgress.value,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun UsageGraphCanvas(
    bars: List<UsageBar>,
    axisMaxUsage: Long,
    isHourly: Boolean,
    showAxisLabels: Boolean,
    revealProgress: Float,
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
    val animatedProgress = revealProgress.coerceIn(0f, 1f)

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

        if (showAxisLabels) {
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
        }

        val barWidth = if (isHourly) 7.dp.toPx() else slotWidth.coerceAtMost(16.dp.toPx())
        val barCornerRadius = 3.dp.toPx()
        bars.forEachIndexed { index, bar ->
            val ratio = (bar.totalTimeMillis.toFloat() / axisMaxUsage.toFloat()).coerceIn(0f, 1f)
            val targetBarHeight = if (bar.totalTimeMillis > 0L) {
                (graphHeight * ratio).coerceAtLeast(barCornerRadius * 2f + 1.dp.toPx())
            } else {
                0f
            }
            val barHeight = targetBarHeight * animatedProgress
            val centerX = left + slotWidth * index + slotWidth / 2f
            val barLeft = centerX - barWidth / 2f
            val barTop = bottom - barHeight
            val color = if (bar.totalTimeMillis > 0L) barColor else emptyColor

            if (barHeight >= 1f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barCornerRadius, barCornerRadius),
                )
            }
        }
    }
}

@Composable
private fun AppUsageList(items: List<AppUsage>) {
    val maxUsage = items.maxOfOrNull { it.totalTimeMillis }?.coerceAtLeast(1L) ?: 1L
    val visibleItems = items.take(8)

    if (items.isEmpty()) {
        RecordCard {
            Text(
                text = "표시할 앱 사용량이 없어요",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSubtle,
            )
        }
    } else {
        AilockCard(contentPadding = PaddingValues(0.dp)) {
            Column {
                visibleItems.forEachIndexed { index, item ->
                    AppUsageRow(item = item, progress = item.totalTimeMillis.toFloat() / maxUsage)
                    if (index != visibleItems.lastIndex) {
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
                    .clip(AILockShape.graphBar)
                    .background(AppSurfaceMuted),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(AILockShape.graphBar)
                        .background(PandaOrange),
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String, modifier: Modifier = Modifier) {
    RecordCard(modifier = modifier) {
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AILockSpacing.cardPadding),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    AilockCard(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        content = content,
    )
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
    val totalMillis = apps.sumOf { it.totalTimeMillis }.takeIf { it > 0L } ?: bars.sumOf { it.totalTimeMillis }
    return UsageSnapshot(bars = bars, apps = apps, totalMillis = totalMillis)
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

private fun HistoryRange.emptyUsageBars(anchorDate: LocalDate): List<UsageBar> =
    when (this) {
        HistoryRange.Day -> (0..23).map { hour ->
            UsageBar(label = hour.toString().padStart(2, '0'), totalTimeMillis = 0L)
        }

        HistoryRange.Week -> (0..6).map { offset ->
            val date = anchorDate.minusDays(6).plusDays(offset.toLong())
            UsageBar(label = "${date.monthValue}/${date.dayOfMonth}", totalTimeMillis = 0L)
        }
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

private data class UsageSnapshot(val bars: List<UsageBar>, val apps: List<AppUsage>, val totalMillis: Long)

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


