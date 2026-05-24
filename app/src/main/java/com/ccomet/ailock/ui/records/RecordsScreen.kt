package com.ccomet.ailock.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.UsageEventType
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AppUsageBar
import com.ccomet.ailock.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(uiState: AILockUiState) {
    var period by remember { mutableStateOf("일간") }
    val records = when (period) {
        "일간" -> TimeUtils.todayRecords(uiState.usageRecords)
        else -> uiState.usageRecords
    }
    val openCount = records.count { it.eventType == UsageEventType.OPEN }
    val totalMinutes = records.sumOf { it.durationMinutes }
    val aiRequests = records.count { it.eventType == UsageEventType.AI_REQUEST }
    val kept = records.count { it.eventType == UsageEventType.SELF_STOP || it.eventType == UsageEventType.CLOSE }
    val broken = records.count { it.eventType == UsageEventType.PLEDGE || it.eventType == UsageEventType.FORCE_HOME }
    val appUsage = records
        .groupBy { it.appName.ifBlank { it.packageName } }
        .mapValues { (_, list) -> list.sumOf { it.durationMinutes }.coerceAtLeast(list.count { it.eventType == UsageEventType.OPEN }) }
        .toList()
        .sortedByDescending { it.second }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = { Text("${uiState.userProfile.name.ifBlank { "사용자" }}님의 기록") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 22.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("일간", "주간", "월간").forEach { label ->
                        FilterChip(
                            selected = period == label,
                            onClick = { period = label },
                            label = { Text(label) },
                        )
                    }
                }
            }
            item {
                SummaryGrid(
                    openCount = openCount,
                    totalMinutes = totalMinutes,
                    topApp = appUsage.firstOrNull()?.first ?: "-",
                    aiRequests = aiRequests,
                    kept = kept,
                    broken = broken,
                )
            }
            item {
                Text("앱별 사용 시간", style = MaterialTheme.typography.titleMedium)
            }
            if (appUsage.isEmpty()) {
                item { EmptyRecordCard() }
            } else {
                items(appUsage, key = { it.first }) { (appName, minutes) ->
                    AppUsageBar(
                        label = appName,
                        minutes = minutes,
                        maxMinutes = (appUsage.firstOrNull()?.second ?: 1).coerceAtLeast(1),
                    )
                }
            }
            item {
                TimelinePlaceholder(period)
            }
        }
    }
}

@Composable
private fun SummaryGrid(
    openCount: Int,
    totalMinutes: Int,
    topApp: String,
    aiRequests: Int,
    kept: Int,
    broken: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("앱을 켠 횟수", "${openCount}회", Modifier.weight(1f))
            SummaryCard("총 사용 시간", TimeUtils.minutesLabel(totalMinutes), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("가장 많이 사용", topApp, Modifier.weight(1f))
            SummaryCard("AI 요청", "${aiRequests}회", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("약속 지킴", "${kept}회", Modifier.weight(1f))
            SummaryCard("약속 어김", "${broken}회", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyRecordCard() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Text(
            "아직 오늘 기록이 없어요. 제한 앱을 설정하면 여기에 쌓입니다.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelinePlaceholder(period: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("타임라인 그래프", style = MaterialTheme.typography.titleMedium)
            Text(
                when (period) {
                    "일간" -> "시간대별 사용량을 간단한 막대로 보여줄 준비가 되어 있어요."
                    "주간" -> "요일별 총 사용량을 이 영역에서 비교합니다."
                    else -> "날짜별 또는 주차별 추이를 이 영역에서 보여줍니다."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
