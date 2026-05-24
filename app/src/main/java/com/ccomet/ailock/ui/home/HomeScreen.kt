package com.ccomet.ailock.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.UsageEventType
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.theme.CreamDeep
import com.ccomet.ailock.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: AILockUiState) {
    val todayRecords = TimeUtils.todayRecords(uiState.usageRecords)
    val openCount = todayRecords.count { it.eventType == UsageEventType.OPEN }
    val aiRequests = todayRecords.count { it.eventType == UsageEventType.AI_REQUEST }
    val selfStops = todayRecords.count { it.eventType == UsageEventType.SELF_STOP }
    val speech = when {
        uiState.willPowerScore >= 80 -> "오늘 꽤 잘하고 있는데?"
        uiState.willPowerScore >= 55 -> "조금 많이 켜고 있어. 천천히 조절하자."
        else -> "지금은 내가 옆에서 조금 더 단호하게 도와줄게."
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("홈") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SpeechBubble(text = speech)
            BigPanda()
            SimpleScore(score = uiState.willPowerScore)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniMetric("실행", "${openCount}회", Modifier.weight(1f))
                MiniMetric("AI 요청", "${aiRequests}회", Modifier.weight(1f))
                MiniMetric("스스로 멈춤", "${selfStops}회", Modifier.weight(1f))
            }
            Text(
                text = "제한 앱 ${uiState.lockedApps.size}개를 같이 지켜보는 중",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BigPanda() {
    Box(
        modifier = Modifier
            .size(236.dp)
            .clip(CircleShape)
            .background(CreamDeep),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.red_panda),
            contentDescription = "레서판다",
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        )
    }
}

@Composable
private fun SimpleScore(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("오늘 약속 지킴 점수", fontWeight = FontWeight.Bold)
                Text("무리하지 말고 한 번씩 멈추기", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
